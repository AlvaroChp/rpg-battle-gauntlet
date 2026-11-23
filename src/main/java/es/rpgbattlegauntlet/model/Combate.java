package es.rpgbattlegauntlet.model;

import es.rpgbattlegauntlet.algorithm.CalculoCombate;
import es.rpgbattlegauntlet.algorithm.InteligenciaEnemiga;
import es.rpgbattlegauntlet.config.CatalogoEnemigos;
import es.rpgbattlegauntlet.enums.EstadoTemporal;
import es.rpgbattlegauntlet.config.CatalogoHabilidades;
import es.rpgbattlegauntlet.enums.ResultadoCombate;
import es.rpgbattlegauntlet.enums.TipoObjetivo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.random.RandomGenerator;
import java.util.stream.Stream;

import static es.rpgbattlegauntlet.enums.ResultadoCombate.*;

public final class Combate {

    private final List<Heroe> heroes;
    private final List<Combatiente> enemigos;
    private final List<Combatiente> participantes;
    private final RandomGenerator azar;
    private final Deque<Combatiente> pendientes = new ArrayDeque<>();
    private final List<EventoCombate> historial = new ArrayList<>();
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

    public synchronized void ejecutar(AccionCombate accion) {
        Combatiente actor = validarTurno(accion.turno(), accion.actor());

        Habilidad habilidad = habilidades(actor).stream()
                .filter(candidata -> candidata.id().equals(accion.habilidad()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("El combatiente no conoce esa habilidad"));
        if (habilidad.costePm() > actor.getPmActuales()) {
            throw new IllegalArgumentException("El combatiente no tiene PM suficientes");
        }
        List<Combatiente> objetivos = objetivos(actor, habilidad.objetivo(), accion.objetivo());

        ejecutarHabilidad(actor, habilidad, objetivos);
        ejecutarTurnosEnemigos();
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
        actor.terminarTurno();
        for (Combatiente objetivo : objetivos) {
            if (!actor.estaVivo()) {
                break;
            }
            registrarConReaccion(actor, objetivo, resolverConsumible(actor, objetivo, objeto));
        }
        consumiblesUsados++;
        avanzarTurno();
        ejecutarTurnosEnemigos();
    }

    private EventoCombate resolverConsumible(Heroe actor, Combatiente objetivo, Objeto objeto) {
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
            }
            case CURACION -> curacion = objetivo.recibirCuracion(efecto.cantidad());
            case CURA_TOTAL -> curacion = objetivo.recibirCuracion(
                    objetivo.getEstadisticasBase().pvMaximos() * efecto.cantidad() / 100);
            case PROTECCION -> { }
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
                tipo(objetivo), danio, curacion, magiaRecuperada, false, false);
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
            var decision = InteligenciaEnemiga.decidir(enemigo, enemigos, heroes);
            UUID objetivo = decision.objetivo() == null ? null : decision.objetivo().getId();
            ejecutarHabilidad(enemigo, decision.habilidad(), objetivos(enemigo, decision.habilidad().objetivo(), objetivo));
            enemigo.registrarAccion();
        }
    }

    private void ejecutarHabilidad(Combatiente actor, Habilidad habilidad, List<Combatiente> objetivos) {
        actor.consumirMagia(habilidad.costePm());
        // Los estados anteriores expiran al actuar. Un estado nuevo empieza después de ese consumo.
        actor.terminarTurno();
        for (Combatiente objetivo : objetivos) {
            for (int golpe = 0; golpe < habilidad.golpes() && actor.estaVivo() && objetivo.estaVivo(); golpe++) {
                registrarConReaccion(actor, objetivo, resolver(actor, objetivo, habilidad));
            }
        }
        avanzarTurno();
    }

    private void registrarConReaccion(Combatiente actor, Combatiente objetivo, EventoCombate evento) {
        historial.add(evento);
        if (evento.danio() > 0 && objetivo.estaVivo()
                && objetivo.retirarEstado(EstadoTemporal.CONTRAATAQUE_LISTO)) {
            historial.add(resolver(objetivo, actor, CatalogoEnemigos.CONTRAATAQUE));
        }
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
            case ALIADO -> List.of(buscarVivo(esHeroe ? heroes : enemigos, objetivo));
            case ENEMIGO -> List.of(buscarVivo(esHeroe ? enemigos : heroes, objetivo));
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

    private Combatiente buscarVivo(List<? extends Combatiente> bando, UUID id) {
        return bando.stream().filter(objetivo -> objetivo.getId().equals(id) && objetivo.estaVivo())
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Objetivo inválido o derrotado"));
    }

    private EventoCombate resolver(Combatiente actor, Combatiente objetivo, Habilidad habilidad) {
        int danio = 0;
        int curacion = 0;
        boolean esquivado = false;
        boolean critico = false;
        switch (habilidad.efecto()) {
            case DANIO_FISICO, DANIO_MAGICO -> {
                int esquiva = Math.max(0, objetivo.getEsquivaEfectiva() - habilidad.precisionAdicional());
                esquivado = azar.nextInt(100) < esquiva;
                if (!esquivado) {
                    critico = azar.nextInt(100) < actor.getEstadisticasBase().critico() + actor.getBonificaciones().critico()
                            + habilidad.criticoAdicional();
                    danio = Math.min(objetivo.getPvActuales(), CalculoCombate.danio(actor, objetivo, habilidad, critico));
                    objetivo.recibirDanio(danio);
                }
            }
            case CURACION -> curacion = objetivo.recibirCuracion(CalculoCombate.curacion(actor, habilidad));
            case ESTADO -> { }
        }
        if (!esquivado && objetivo.estaVivo() && habilidad.estado() != null) {
            objetivo.aplicarEstado(habilidad.estado(), habilidad.duracion());
        }
        return new EventoCombate(turno, actor.getId(), tipo(actor), habilidad.id(), objetivo.getId(),
                tipo(objetivo), danio, curacion, 0, esquivado, critico);
    }

    private void avanzarTurno() {
        pendientes.remove();
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
