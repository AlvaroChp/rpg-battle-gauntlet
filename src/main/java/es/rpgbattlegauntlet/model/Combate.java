package es.rpgbattlegauntlet.model;

import es.rpgbattlegauntlet.algorithm.CalculoCombate;
import es.rpgbattlegauntlet.algorithm.GeneradorBotin;
import es.rpgbattlegauntlet.algorithm.InteligenciaEnemiga;
import es.rpgbattlegauntlet.config.CatalogoEnemigos;
import es.rpgbattlegauntlet.enums.EstadoTemporal;
import es.rpgbattlegauntlet.config.CatalogoHabilidades;
import es.rpgbattlegauntlet.enums.ResultadoCombate;
import es.rpgbattlegauntlet.enums.TipoObjetivo;
import es.rpgbattlegauntlet.enums.TipoEnemigo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.random.RandomGenerator;
import java.util.stream.Stream;

import static es.rpgbattlegauntlet.enums.ResultadoCombate.*;
import static es.rpgbattlegauntlet.enums.EstadoTemporal.*;
import static es.rpgbattlegauntlet.enums.TipoEfecto.DANIO_FISICO;
import static es.rpgbattlegauntlet.enums.TipoEfecto.DANIO_MAGICO;
import static es.rpgbattlegauntlet.enums.TipoConsumible.ANIQUILAR_DRAGON;
import static es.rpgbattlegauntlet.enums.TipoEfecto.ROBO;

public final class Combate {

    private final List<Heroe> heroes;
    private final List<Combatiente> enemigos;
    private final List<Combatiente> participantes;
    private final RandomGenerator azar;
    private final Deque<Combatiente> pendientes = new ArrayDeque<>();
    private final List<EventoCombate> historial = new ArrayList<>();
    private final Set<UUID> objetivosRobados = new HashSet<>();
    private ResultadoCombate resultado = EN_CURSO;
    private final int primerTurno;
    private int turno;
    private int consumiblesUsados;

    public Combate(List<Heroe> heroes, List<? extends Combatiente> enemigos, RandomGenerator azar) {
        this(heroes, enemigos, azar, 1);
    }

    Combate(List<Heroe> heroes, List<? extends Combatiente> enemigos, RandomGenerator azar, int primerTurno) {
        this.turno = primerTurno;
        this.primerTurno = primerTurno;
        this.heroes = List.copyOf(heroes);
        this.enemigos = List.copyOf(enemigos);
        this.azar = Objects.requireNonNull(azar, "La fuente de azar es obligatoria");
        participantes = Stream.<Combatiente>concat(this.heroes.stream(), this.enemigos.stream()).toList();
        if (this.heroes.stream().noneMatch(Combatiente::estaVivo)
                || this.enemigos.stream().noneMatch(Combatiente::estaVivo)) {
            throw new IllegalArgumentException("Cada bando necesita al menos un combatiente vivo");
        }
        if (new HashSet<>(participantes).size() != participantes.size()) {
            throw new IllegalArgumentException("Un combatiente solo puede participar una vez");
        }
        ordenarCiclo();
        ejecutarTurnosEnemigos();
    }

    public List<Combatiente> getEnemigos() {
        return enemigos;
    }

    public synchronized int getTurno() {
        return turno;
    }

    public synchronized int getCantidadTurnos() {
        return turno - primerTurno + (resultado == EN_CURSO ? 0 : 1);
    }

    public synchronized int getConsumiblesUsados() {
        return consumiblesUsados;
    }

    public synchronized ResultadoCombate getResultado() {
        return resultado;
    }

    public synchronized List<Combatiente> getOrdenActuacion() {
        return List.copyOf(pendientes);
    }

    public synchronized List<EventoCombate> getHistorial() {
        return List.copyOf(historial);
    }

    public synchronized List<Habilidad> getHabilidadesDisponibles() {
        Combatiente actor = pendientes.peek();
        if (actor == null) {
            return List.of();
        }
        return habilidades(actor).stream().filter(habilidad -> habilidad.costePm() <= actor.getPmActuales()).toList();
    }

    public synchronized boolean fueRobado(UUID objetivo) {
        return objetivosRobados.contains(objetivo);
    }

    public synchronized boolean puedeSerRobado(Combatiente objetivo) {
        return objetivo instanceof Enemigo enemigo && enemigo.estaVivo()
                && GeneradorBotin.permiteRobo(enemigo.getTipo()) && !fueRobado(enemigo.getId());
    }

    public synchronized Objeto ejecutar(AccionCombate accion) {
        Combatiente actor = validarTurno(accion.turno(), accion.actor());

        Habilidad habilidad = habilidades(actor).stream()
                .filter(candidata -> candidata.id().equals(accion.habilidad()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("El combatiente no conoce esa habilidad"));
        if (habilidad.costePm() > actor.getPmActuales()) {
            throw new IllegalArgumentException("El combatiente no tiene PC suficientes");
        }
        List<Combatiente> objetivos = objetivos(actor, habilidad.objetivo(), accion.objetivo());
        boolean robo = habilidad.efecto() == ROBO;
        if (robo && !puedeSerRobado(objetivos.getFirst())) {
            throw new IllegalArgumentException("Este enemigo no tiene ningún objeto que puedas robar");
        }

        int primerEvento = historial.size();
        ejecutarHabilidad(actor, habilidad, objetivos);
        Objeto objetoRobado = robo ? historial.get(primerEvento).objetoRobado() : null;
        ejecutarTurnosEnemigos();
        return objetoRobado;
    }

    synchronized void usarConsumible(int numeroTurno, UUID actorId, Objeto objeto, UUID objetivoId) {
        if (!(validarTurno(numeroTurno, actorId) instanceof Heroe actor) || objeto.efecto() == null) {
            throw new IllegalArgumentException("La acción necesita un héroe y un consumible");
        }
        if (!objeto.cumpleRequisitos(actor)) {
            throw new IllegalArgumentException("Inteligencia base insuficiente");
        }

        var efecto = objeto.efecto();
        var objetivos = objetivos(actor, efecto.objetivo(), objetivoId);
        if (efecto.tipo() == ANIQUILAR_DRAGON && (!(objetivos.getFirst() instanceof Enemigo enemigo)
                || enemigo.getTipo() != TipoEnemigo.DRAGON)) {
            throw new IllegalArgumentException("El Pergamino de Dragón solo puede usarse contra el dragón");
        }
        ejecutarConsumible(actor, objeto, objetivos);
        ejecutarTurnosEnemigos();
    }

    private void ejecutarConsumible(Combatiente actor, Objeto objeto, List<Combatiente> objetivos) {
        var daniosPendientes = actor.terminarTurno();
        for (Combatiente objetivo : objetivos) {
            if (!actor.estaVivo()) {
                break;
            }
            registrarConReaccion(actor, objetivo, resolverConsumible(actor, objetivo, objeto));
        }
        if (actor instanceof Heroe) consumiblesUsados++;
        avanzarTurno(daniosPendientes);
    }

    private EventoCombate resolverConsumible(Combatiente actor, Combatiente objetivo, Objeto objeto) {
        var efecto = objeto.efecto();
        int danio = 0;
        int curacion = 0;
        int magiaRecuperada = 0;
        switch (efecto.tipo()) {
            case POCION -> {
                curacion = objetivo.recibirCuracion(efecto.cantidad());
                magiaRecuperada = objetivo.recuperarMagia(efecto.magia());
            }
            case DANIO -> {
                danio = Math.min(objetivo.getPvActuales(), CalculoCombate.danioPergamino(objetivo, efecto.cantidad()));
                objetivo.recibirDanio(danio);
                if (efecto.incendiario()) aplicarEstado(objetivo, QUEMADO, 2, 50, false);
            }
            case CURACION -> curacion = objetivo.recibirCuracion(efecto.cantidad());
            case CURA_TOTAL -> curacion = objetivo.recibirCuracion(
                    objetivo.getEstadisticasBase().pvMaximos() * efecto.cantidad() / 100);
            case PROTECCION -> { }
            case REVITALIZACION -> curacion = objetivo.revivir(efecto.cantidad());
            case ANIQUILAR_DRAGON -> {
                danio = objetivo.getPvActuales();
                objetivo.recibirDanio(danio);
            }
        }
        if (efecto.proteccion() > 0) {
            EstadoTemporal proteccion = switch (efecto.proteccion()) {
                case 35 -> EstadoTemporal.PROTECCION_35;
                case 50 -> EstadoTemporal.PROTECCION_50;
                case 65 -> EstadoTemporal.PROTECCION_65;
                default -> throw new IllegalStateException("Protección sin estado asociado");
            };
            objetivo.aplicarEstado(proteccion, 2);
        }
        return new EventoCombate(turno, actor.getId(), tipo(actor), objeto.nombre(), objetivo.getId(),
                tipo(objetivo), danio, curacion, magiaRecuperada, false, false, null);
    }

    private String tipo(Combatiente combatiente) {
        return switch (combatiente) {
            case Heroe heroe -> heroe.getClase().name();
            case Enemigo enemigo -> enemigo.getTipo().name();
            default -> "Combatiente";
        };
    }

    private Combatiente validarTurno(int numero, UUID id) {
        if (resultado != EN_CURSO) {
            throw new IllegalStateException("El combate ha terminado");
        }
        Combatiente actor = pendientes.element();
        if (numero != turno || !actor.getId().equals(id)) {
            throw new IllegalArgumentException("La acción no corresponde al turno actual");
        }

        return actor;
    }

    private void ejecutarTurnosEnemigos() {
        while (pendientes.peek() instanceof Enemigo enemigo) {
            Combatiente caido = InteligenciaEnemiga.aliadoParaRevitalizar(enemigo, enemigos);
            if (caido != null) {
                ejecutarConsumible(enemigo, enemigo.retirarPocionRevitalizacion(), List.of(caido));
            } else {
                var decision = InteligenciaEnemiga.decidir(enemigo, enemigos, heroes, azar);
                UUID objetivo = decision.objetivo() == null ? null : decision.objetivo().getId();
                ejecutarHabilidad(enemigo, decision.habilidad(), objetivos(enemigo, decision.habilidad().objetivo(), objetivo));
            }
            enemigo.registrarAccion();
        }
    }

    private void ejecutarHabilidad(Combatiente actor, Habilidad habilidad, List<Combatiente> objetivos) {
        actor.consumirMagia(habilidad.costePm());
        if (actor instanceof Enemigo enemigo
                && (habilidad.estado() == CARGA_FURIA_ANIMAL || habilidad.estado() == CARGA_INCENDIO_CENIZA)) {
            enemigo.iniciarAtaqueUnico();
        }
        // Los estados anteriores expiran al actuar. Un estado nuevo empieza después de ese consumo.
        var daniosPendientes = actor.terminarTurno();
        for (Combatiente objetivo : objetivos) {
            for (int golpe = 0; golpe < habilidad.golpes() && actor.estaVivo() && objetivo.estaVivo(); golpe++) {
                Combatiente receptor = interceptarSiProcede(actor, objetivo, habilidad);
                registrarConReaccion(actor, receptor, resolver(actor, receptor, habilidad));
            }
        }
        avanzarTurno(daniosPendientes);
    }

    private void registrarConReaccion(Combatiente actor, Combatiente objetivo, EventoCombate evento) {
        historial.add(evento);
        if (evento.critico() && objetivo.estaVivo() && objetivo.retirarEstado(CARGA_FURIA_ANIMAL)) {
            historial.add(new EventoCombate(turno, objetivo.getId(), tipo(objetivo), "furia_interrumpida", objetivo.getId(),
                    tipo(objetivo), 0, 0, 0, false, false, null));
        }
        if (evento.danio() > 0 && objetivo.estaVivo()
                && objetivo.retirarEstado(EstadoTemporal.CONTRAATAQUE_LISTO)) {
            var contraataque = CatalogoEnemigos.CONTRAATAQUE;
            historial.add(resolver(objetivo, interceptarSiProcede(objetivo, actor, contraataque), contraataque));
        }
    }

    private Combatiente interceptarSiProcede(Combatiente actor, Combatiente objetivo, Habilidad habilidad) {
        if (habilidad.objetivo() != TipoObjetivo.ENEMIGO || !esAtaque(habilidad)
                || !heroes.contains(objetivo) || heroes.contains(actor) || objetivo.tieneEstado(INTERCEPTANDO)) {
            return objetivo;
        }
        var interceptor = heroes.stream().filter(Heroe::estaVivo).filter(heroe -> heroe.tieneEstado(INTERCEPTANDO)).findFirst();
        if (interceptor.isEmpty()) return objetivo;

        Heroe guerrero = interceptor.get();
        historial.add(new EventoCombate(turno, guerrero.getId(), tipo(guerrero), "golpe_interceptado", objetivo.getId(),
                tipo(objetivo), 0, 0, 0, false, false, null));
        return guerrero;
    }

    private static boolean esAtaque(Habilidad habilidad) {
        return habilidad.efecto() == DANIO_FISICO || habilidad.efecto() == DANIO_MAGICO;
    }

    private List<Habilidad> habilidades(Combatiente actor) {
        var opciones = new ArrayList<>(List.of(CatalogoHabilidades.ATAQUE_BASICO, CatalogoHabilidades.DEFENDER));
        if (actor instanceof Heroe heroe) {
            opciones.addAll(CatalogoHabilidades.deClase(heroe.getClase()));
            Habilidad arma = heroe.habilidadArma();
            if (arma != null) {
                opciones.add(arma);
            }
        }
        return opciones;
    }

    private List<Combatiente> objetivos(Combatiente actor, TipoObjetivo tipo, UUID objetivo) {
        boolean esHeroe = heroes.contains(actor);
        return switch (tipo) {
            case PROPIO -> {
                if (objetivo != null && !actor.getId().equals(objetivo)) {
                    throw new IllegalArgumentException("La habilidad solo se aplica al propio combatiente");
                }
                yield List.of(actor);
            }
            case ALIADO -> List.of(buscarObjetivo(esHeroe ? heroes : enemigos, objetivo, true));
            case ALIADO_CAIDO -> List.of(buscarObjetivo(esHeroe ? heroes : enemigos, objetivo, false));
            case ENEMIGO -> List.of(buscarObjetivo(esHeroe ? enemigos : heroes, objetivo, true));
            case ENEMIGOS, ALIADOS -> {
                if (objetivo != null) {
                    throw new IllegalArgumentException("Las habilidades de área no requieren un objetivo individual");
                }
                boolean objetivoHeroe = tipo == TipoObjetivo.ALIADOS ? esHeroe : !esHeroe;
                List<? extends Combatiente> bando = objetivoHeroe ? heroes : enemigos;
                yield bando.stream().filter(Combatiente::estaVivo).map(Combatiente.class::cast).toList();
            }
        };
    }

    private Combatiente buscarObjetivo(List<? extends Combatiente> bando, UUID id, boolean vivo) {
        return bando.stream().filter(objetivo -> objetivo.getId().equals(id) && objetivo.estaVivo() == vivo)
                .findFirst().orElseThrow(() -> new IllegalArgumentException(
                        vivo ? "Objetivo inválido o derrotado" : "El objetivo debe ser un aliado caído"));
    }

    private EventoCombate resolver(Combatiente actor, Combatiente objetivo, Habilidad habilidad) {
        int danio = 0;
        int curacion = 0;
        boolean esquivado = false;
        boolean critico = false;
        Objeto objetoRobado = null;
        boolean interceptando = esAtaque(habilidad) && objetivo.tieneEstado(INTERCEPTANDO) && !heroes.contains(actor);
        int reduccionMinima = interceptando ? objetivo.reduccionAlDefenderse() : 0;
        switch (habilidad.efecto()) {
            case DANIO_FISICO -> {
                int esquiva = Math.max(0, objetivo.getEsquivaEfectiva() - habilidad.precisionAdicional());
                esquivado = azar.nextInt(100) < esquiva;
                if (!esquivado) {
                    var base = actor.getEstadisticasBase();
                    int criticoBase = actor instanceof Heroe ? base.destreza() : base.critico();
                    critico = azar.nextInt(100) < criticoBase + actor.getBonificaciones().critico() + habilidad.criticoAdicional();
                    danio = Math.min(objetivo.getPvActuales(), CalculoCombate.danio(actor, objetivo, habilidad, critico, reduccionMinima));
                    objetivo.recibirDanio(danio);
                }
            }
            case DANIO_MAGICO -> {
                danio = Math.min(objetivo.getPvActuales(), CalculoCombate.danio(actor, objetivo, habilidad, false, reduccionMinima));
                objetivo.recibirDanio(danio);
            }
            case CURACION -> curacion = objetivo.recibirCuracion(CalculoCombate.curacion(actor, habilidad));
            case ESTADO -> { }
            case ROBO -> {
                if (azar.nextInt(100) < ((Heroe) actor).probabilidadRobo()) {
                    Enemigo enemigo = (Enemigo) objetivo;
                    objetoRobado = enemigo.retirarPocionRevitalizacion();
                    if (objetoRobado == null) objetoRobado = GeneradorBotin.elegirBotinRobado(enemigo.getTipo(), azar);
                    objetivosRobados.add(objetivo.getId());
                }
            }
        }
        if (!esquivado && habilidad.estado() != null) {
            aplicarEstado(objetivo, habilidad.estado(), habilidad.duracion(), habilidad.probabilidadEstado(), interceptando);
        }
        if (interceptando && danio > 0) objetivo.retirarEstado(INTERCEPTANDO);
        return new EventoCombate(turno, actor.getId(), tipo(actor), habilidad.id(), objetivo.getId(),
                tipo(objetivo), danio, curacion, 0, esquivado, critico, objetoRobado);
    }

    private void aplicarEstado(Combatiente objetivo, EstadoTemporal estado, int duracion, int probabilidad, boolean interceptando) {
        if (!objetivo.estaVivo()) return;
        boolean protegido = interceptando || objetivo.tieneEstado(DEFENSA) || objetivo.tieneEstado(BENDICION);
        int efectiva = estado.danioPeriodico() > 0 && protegido ? probabilidad / 2 : probabilidad;
        if (efectiva >= 100 || azar.nextInt(100) < efectiva) objetivo.aplicarEstado(estado, duracion);
    }

    private void avanzarTurno(List<EstadoTemporal> daniosPendientes) {
        Combatiente actor = pendientes.remove();
        if (!daniosPendientes.isEmpty() && actor.estaVivo() && heroes.stream().anyMatch(Combatiente::estaVivo)
                && enemigos.stream().anyMatch(Combatiente::estaVivo)) {
            for (EstadoTemporal estado : daniosPendientes) {
                int danio = Math.min(actor.getPvActuales(), estado.danioPeriodico());
                actor.recibirDanio(danio);
                historial.add(new EventoCombate(turno, actor.getId(), tipo(actor), estado.eventoDanio(), actor.getId(),
                        tipo(actor), danio, 0, 0, false, false, null));
                if (!actor.estaVivo()) break;
            }
        }
        if (enemigos.stream().noneMatch(Combatiente::estaVivo)) {
            resultado = GANADO;
        } else if (heroes.stream().noneMatch(Combatiente::estaVivo)) {
            resultado = PERDIDO;
        }
        if (resultado != EN_CURSO) {
            pendientes.clear();
            participantes.forEach(Combatiente::limpiarEstados);
            return;
        }

        turno++;
        pendientes.removeIf(combatiente -> !combatiente.estaVivo());
        if (pendientes.isEmpty()) {
            ordenarCiclo();
        }
    }

    private void ordenarCiclo() {
        participantes.stream().filter(Combatiente::estaVivo)
                .sorted(Comparator.comparingInt(Combatiente::getVelocidadEfectiva).reversed())
                .forEachOrdered(pendientes::addLast);
    }
}
