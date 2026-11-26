package es.rpgbattlegauntlet.model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.random.RandomGenerator;
import java.util.stream.Stream;

import es.rpgbattlegauntlet.algorithm.CalculoCombate;
import es.rpgbattlegauntlet.algorithm.GeneradorBotin;
import es.rpgbattlegauntlet.algorithm.InteligenciaEnemiga;
import es.rpgbattlegauntlet.config.CatalogoEnemigos;
import es.rpgbattlegauntlet.config.CatalogoHabilidades;
import es.rpgbattlegauntlet.enums.ClaseHeroe;
import es.rpgbattlegauntlet.enums.EstadoTemporal;
import static es.rpgbattlegauntlet.enums.EstadoTemporal.BENDICION;
import static es.rpgbattlegauntlet.enums.EstadoTemporal.CARGA_FURIA_ANIMAL;
import static es.rpgbattlegauntlet.enums.EstadoTemporal.CARGA_INCENDIO_CENIZA;
import static es.rpgbattlegauntlet.enums.EstadoTemporal.CARGA_RUPTURA_VINCULO;
import static es.rpgbattlegauntlet.enums.EstadoTemporal.CORAZA_AGRIETADA;
import static es.rpgbattlegauntlet.enums.EstadoTemporal.DEFENSA;
import static es.rpgbattlegauntlet.enums.EstadoTemporal.ESCARCHA;
import static es.rpgbattlegauntlet.enums.EstadoTemporal.INTERCEPTANDO;
import static es.rpgbattlegauntlet.enums.EstadoTemporal.MARCA_ESPECTRAL;
import static es.rpgbattlegauntlet.enums.EstadoTemporal.QUEMADO;
import static es.rpgbattlegauntlet.enums.EstadoTemporal.VINCULO_ROTO;
import es.rpgbattlegauntlet.enums.ResultadoCombate;
import static es.rpgbattlegauntlet.enums.ResultadoCombate.EN_CURSO;
import static es.rpgbattlegauntlet.enums.ResultadoCombate.GANADO;
import static es.rpgbattlegauntlet.enums.ResultadoCombate.PERDIDO;
import static es.rpgbattlegauntlet.enums.TipoConsumible.ANIQUILAR_DRAGON;
import es.rpgbattlegauntlet.enums.TipoEfecto;
import static es.rpgbattlegauntlet.enums.TipoEfecto.DANIO_FISICO;
import static es.rpgbattlegauntlet.enums.TipoEfecto.DANIO_MAGICO;
import static es.rpgbattlegauntlet.enums.TipoEfecto.ROBO;
import es.rpgbattlegauntlet.enums.TipoEnemigo;
import es.rpgbattlegauntlet.enums.TipoObjetivo;

public final class Combate {

    private final List<Heroe> heroes;
    private final List<Combatiente> enemigos;
    private final List<Combatiente> participantes;
    private final RandomGenerator azar;
    private final boolean modoPruebas;
    private final List<Enemigo> refuerzos;
    private final Enemigo azrathTrasladado;
    private final int accionesPreviasDeAzrath;
    private final Deque<Combatiente> pendientes = new ArrayDeque<>();
    private final List<EventoCombate> historial = new ArrayList<>();
    private final Set<UUID> objetivosRobados = new HashSet<>();
    private ResultadoCombate resultado = EN_CURSO;
    private final int primerTurno;
    private int turno;
    private int consumiblesUsados;

    public Combate(List<Heroe> heroes, List<? extends Combatiente> enemigos, RandomGenerator azar) {
        this(heroes, enemigos, azar, 1, false);
    }

    Combate(List<Heroe> heroes, List<? extends Combatiente> enemigos, RandomGenerator azar, int primerTurno, boolean modoPruebas) {
        this(heroes, enemigos, azar, primerTurno, modoPruebas, List.of());
    }

    Combate(List<Heroe> heroes, List<? extends Combatiente> enemigos, RandomGenerator azar, int primerTurno,
            boolean modoPruebas, List<Enemigo> refuerzos) {
        this.turno = primerTurno;
        this.primerTurno = primerTurno;
        this.heroes = List.copyOf(heroes);
        this.modoPruebas = modoPruebas;
        this.enemigos = new ArrayList<>(enemigos);
        this.refuerzos = new ArrayList<>(refuerzos);
        azrathTrasladado = refuerzos.isEmpty() ? null : (Enemigo) enemigos.getFirst();
        accionesPreviasDeAzrath = azrathTrasladado == null ? 0 : azrathTrasladado.getAccionesRealizadas();
        this.azar = Objects.requireNonNull(azar, "La fuente de azar es obligatoria");
        participantes = new ArrayList<>(Stream.<Combatiente>concat(this.heroes.stream(), this.enemigos.stream()).toList());
        if (this.heroes.stream().noneMatch(Combatiente::estaVivo)
                || this.enemigos.stream().noneMatch(Combatiente::estaVivo)) {
            throw new IllegalArgumentException("Cada bando necesita al menos un combatiente vivo");
        }
        if (new HashSet<>(participantes).size() != participantes.size()) {
            throw new IllegalArgumentException("Un combatiente solo puede participar una vez");
        }
        ordenarCiclo();
        for (var enemigo : this.enemigos) {
            if (enemigo.estaVivo() && (enemigo != azrathTrasladado || primerTurno == 1)) {
                registrarDialogoJefe(enemigo, true);
            }
        }
        ejecutarTurnosEnemigos();
    }

    public synchronized List<Combatiente> getEnemigos() {
        return List.copyOf(enemigos);
    }

    public synchronized int getTurno() {
        return turno;
    }

    public int getPrimerTurno() {
        return primerTurno;
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
        return habilidadesDelTurno(true);
    }

    public synchronized List<Habilidad> getHabilidadesDelTurno() {
        return habilidadesDelTurno(false);
    }

    private List<Habilidad> habilidadesDelTurno(boolean soloPagables) {
        Combatiente actor = pendientes.peek();
        if (actor == null) {
            return List.of();
        }
        return habilidades(actor).stream().filter(habilidad -> !soloPagables || costeReal(actor, habilidad) <= actor.getPmActuales())
                .map(habilidad -> habilidad.conCoste(actor.costeHabilidad(habilidad))).toList();
    }

    public synchronized boolean fueRobado(UUID objetivo) {
        return objetivosRobados.contains(objetivo);
    }

    public synchronized boolean puedeSerRobado(Combatiente objetivo) {
        return objetivo instanceof Enemigo enemigo && enemigo != azrathTrasladado && enemigo.estaVivo()
                && GeneradorBotin.permiteRobo(enemigo.getTipo()) && !fueRobado(enemigo.getId());
    }

    public synchronized Objeto ejecutar(AccionCombate accion) {
        Combatiente actor = validarTurno(accion.turno(), accion.actor());

        Habilidad habilidad = habilidades(actor).stream()
                .filter(candidata -> candidata.id().equals(accion.habilidad()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("El combatiente no conoce esa habilidad"));
        if (costeReal(actor, habilidad) > actor.getPmActuales()) {
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
        usarConsumible(numeroTurno, actorId, objeto, objetivoId, false);
    }

    synchronized Enemigo usarConsumible(int numeroTurno, UUID actorId, Objeto objeto, UUID objetivoId, boolean trasladarAzrath) {
        if (!(validarTurno(numeroTurno, actorId) instanceof Heroe actor) || objeto.efecto() == null) {
            throw new IllegalArgumentException("La acción necesita un héroe y un consumible");
        }
        if (!objeto.cumpleRequisitos(actor, modoPruebas)) {
            throw new IllegalArgumentException("Inteligencia base insuficiente");
        }

        var efecto = objeto.efecto();
        var objetivos = objetivos(actor, efecto.objetivo(), objetivoId);
        objetivos.forEach(efecto::validarRecuperacion);
        if (efecto.tipo() == ANIQUILAR_DRAGON && (!(objetivos.getFirst() instanceof Enemigo enemigo)
                || enemigo.getTipo() != TipoEnemigo.DRAGON || enemigo == azrathTrasladado)) {
            throw new IllegalArgumentException("El Pergamino de Dragón solo puede usarse contra el dragón");
        }
        if (efecto.tipo() == ANIQUILAR_DRAGON) {
            registrarDialogo(objetivos.getFirst(), "dialogo_pergamino", "¡¿Qué has hecho?! ¡¡NOOOOOO!!");
        }
        if (trasladarAzrath) {
            var azrath = (Enemigo) objetivos.getFirst();
            int danio = infligirDanio(azrath, azrath.getPvActuales() - Math.max(1, azrath.getPvActuales() / 2));
            historial.add(new EventoCombate(turno, actor.getId(), tipo(actor), "traslado_azrath", azrath.getId(),
                    tipo(azrath), danio, 0, 0, false, false, null));
            return azrath;
        }
        ejecutarConsumible(actor, objeto, objetivos);
        ejecutarTurnosEnemigos();
        return null;
    }

    private void ejecutarConsumible(Combatiente actor, Objeto objeto, List<Combatiente> objetivos) {
        int primerEvento = historial.size();
        var antes = new EstadoVisual(actor);
        var daniosPendientes = actor.terminarTurno();
        var ajuste = antes.cambio();
        for (Combatiente objetivo : objetivos) {
            if (!actor.estaVivo()) {
                break;
            }
            registrarConReaccion(actor, objetivo, resolverConsumible(actor, objetivo, objeto));
        }
        if (actor instanceof Heroe) consumiblesUsados++;
        registrarAjustes(primerEvento, List.of(ajuste), true);
        avanzarTurno(daniosPendientes);
    }

    private EventoCombate resolverConsumible(Combatiente actor, Combatiente objetivo, Objeto objeto) {
        var estadosAntes = objetivo.getEstados();
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
                danio = infligirDanio(objetivo, CalculoCombate.danioPergamino(objetivo, efecto.cantidad(), efecto.incendiario()));
                if (efecto.incendiario()) aplicarEstado(objetivo, QUEMADO, 2, 50, false);
                if (efecto.escarcha() && objetivo.estaVivo()) objetivo.aplicarEstado(ESCARCHA, 1);
            }
            case CURACION -> curacion = objetivo.recibirCuracion(efecto.cantidad());
            case CURA_TOTAL -> curacion = objetivo.recibirCuracion(
                    objetivo.getEstadisticasBase().pvMaximos() * efecto.cantidad() / 100);
            case PROTECCION -> objetivo.aplicarEstado(EstadoTemporal.PROTECCION_50, 2);
            case REVITALIZACION -> curacion = objetivo.revivir(efecto.cantidad());
            case ANIQUILAR_DRAGON -> {
                danio = infligirDanio(objetivo, objetivo.getPvActuales());
            }
        }
        return new EventoCombate(turno, actor.getId(), tipo(actor), objeto.nombre(), objetivo.getId(),
                tipo(objetivo), danio, curacion, magiaRecuperada, false, false, null, 0,
                EventoCombate.CambioEstados.entre(estadosAntes, objetivo.getEstados()));
    }

    private String tipo(Combatiente combatiente) {
        return switch (combatiente) {
            case Heroe heroe -> heroe.getClase().name();
            case Enemigo enemigo -> enemigo.getTipo().name();
            default -> "Combatiente";
        };
    }

    private void registrarDialogoJefe(Combatiente combatiente, boolean inicio) {
        if (!(combatiente instanceof Enemigo enemigo)) return;
        String frase = switch (enemigo.getTipo()) {
            case OGRO -> inicio ? "Amo dice que yo no deja pasar a nadie"
                    : "yo falla mision, ¡Azrath no perdona a mi!";
            case VERDUGO_ESPECTRAL -> inicio ? "Segar almas es mi cometido mientras siga atado a este mundo."
                    : "Al fin estoy libre de mis cadenas, os deseo suerte héroes, la necesitaréis.";
            case GOLEM_RUNICO -> {
                boolean vaelthar = heroes.stream().anyMatch(heroe -> heroe.getClase() == ClaseHeroe.MAGO);
                yield inicio ? vaelthar ? "Vaelthar, parece que el tiempo no te ha tratado bien."
                        : "No podéis continuar, no puedo permitiroslo."
                        : vaelthar ? "Buena suerte Vaelthar, lamento lo que le ocurrió a los tuyos"
                        : "Mis runas se debilitan, la magia que me mueve ya no funciona";
            }
            case DRAGON -> inicio ? "No sabéis ante quién estáis, ¿verdad? ¡os calcinaré hasta que no queden más que cenizas!"
                    : "¡No! Es imposible... ¡Soy el más poderoso de todo Aetherion! ¡Soy el más pode... ARGH!";
            default -> null;
        };
        if (frase != null) registrarDialogo(enemigo, inicio ? "dialogo_inicio" : "dialogo_muerte", frase);
    }

    private void registrarDialogo(Combatiente actor, String evento, String frase) {
        historial.add(new EventoCombate(turno, actor.getId(), tipo(actor), evento, actor.getId(),
                tipo(actor), 0, 0, 0, false, false, null, 0, null, frase));
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

    private int costeReal(Combatiente actor, Habilidad habilidad) {
        return modoPruebas && actor instanceof Heroe ? 0 : actor.costeHabilidad(habilidad);
    }

    private void ejecutarHabilidad(Combatiente actor, Habilidad habilidad, List<Combatiente> objetivos) {
        int primerEvento = historial.size();
        var antes = new EstadoVisual(actor);
        actor.consumirMagia(costeReal(actor, habilidad));
        if (actor instanceof Enemigo enemigo
                && (habilidad.estado() == CARGA_FURIA_ANIMAL || habilidad.estado() == CARGA_INCENDIO_CENIZA
                || habilidad.estado() == CARGA_RUPTURA_VINCULO)) {
            enemigo.iniciarAtaqueUnico();
        }
        if (actor instanceof Enemigo verdugo && verdugo.getObjetivoMarcado() != null) {
            verdugo.marcarObjetivo(null);
        }
        // Los estados anteriores expiran al actuar. Un estado nuevo empieza después de ese consumo.
        var daniosPendientes = actor.terminarTurno();
        var ajuste = antes.cambio();
        for (Combatiente objetivo : objetivos) {
            for (int golpe = 0; golpe < habilidad.golpes() && actor.estaVivo()
                    && (objetivo.estaVivo() || habilidad.efecto() == TipoEfecto.REVIVIR); golpe++) {
                Combatiente receptor = interceptarSiProcede(actor, objetivo, habilidad);
                registrarConReaccion(actor, receptor, resolver(actor, receptor, habilidad));
            }
        }
        registrarAjustes(primerEvento, List.of(ajuste), true);
        var antesDelCierre = new EstadoVisual(actor);
        if (habilidad == CatalogoEnemigos.DERRUMBE) actor.aplicarEstado(CORAZA_AGRIETADA, 1);
        if (habilidad == CatalogoEnemigos.RUPTURA) actor.aplicarEstado(VINCULO_ROTO, 1);
        if (historial.size() > primerEvento) {
            registrarAjustes(historial.size() - 1, List.of(antesDelCierre.cambio()), false);
        }
        avanzarTurno(daniosPendientes);
    }

    private record EstadoVisual(Combatiente combatiente, int pc, Map<EstadoTemporal, Integer> estados) {
        EstadoVisual(Combatiente combatiente) {
            this(combatiente, combatiente.getPmActuales(), combatiente.getEstados());
        }

        EventoCombate.Ajuste cambio() {
            return new EventoCombate.Ajuste(combatiente.getId(), combatiente.getPmActuales() - pc,
                    EventoCombate.CambioEstados.entre(estados, combatiente.getEstados()));
        }
    }

    private void registrarAjustes(int indice, List<EventoCombate.Ajuste> ajustes, boolean previos) {
        if (indice < 0 || indice >= historial.size()) return;
        var cambios = ajustes.stream().filter(ajuste -> ajuste.cambioPc() != 0 || ajuste.estados() != null).toList();
        if (!cambios.isEmpty()) {
            var evento = historial.get(indice);
            var combinados = new ArrayList<>(previos ? evento.ajustesPrevios() : evento.ajustesPosteriores());
            combinados.addAll(cambios);
            historial.set(indice, evento.conAjustes(List.copyOf(combinados), previos));
        }
    }

    private void registrarConReaccion(Combatiente actor, Combatiente objetivo, EventoCombate evento) {
        historial.add(evento);
        if (evento.objetoRobado() != null && "requiem_dragon".equals(evento.objetoRobado().codigo())) {
            registrarDialogo(objetivo, "dialogo_robo", "¡Devuélveme eso, gusano! ¡No tienes ni idea de lo que es!");
        }
        if (evento.danio() > 0 && !objetivo.estaVivo()) registrarDialogoJefe(objetivo, false);
        var antes = new EstadoVisual(objetivo);
        if (evento.critico() && objetivo.estaVivo() && objetivo.retirarEstado(CARGA_FURIA_ANIMAL)) {
            historial.add(new EventoCombate(turno, objetivo.getId(), tipo(objetivo), "furia_interrumpida", objetivo.getId(),
                    tipo(objetivo), 0, 0, 0, false, false, null));
            registrarAjustes(historial.size() - 1, List.of(antes.cambio()), true);
            antes = new EstadoVisual(objetivo);
        }
        if (evento.danio() > 0 && objetivo.estaVivo()
                && objetivo.retirarEstado(EstadoTemporal.CONTRAATAQUE_LISTO)) {
            var contraataque = CatalogoEnemigos.CONTRAATAQUE;
            historial.add(resolver(objetivo, interceptarSiProcede(objetivo, actor, contraataque), contraataque));
            registrarAjustes(historial.size() - 1, List.of(antes.cambio()), true);
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
        Habilidad basico = actor instanceof Heroe heroe ? heroe.ataqueBasico() : CatalogoHabilidades.ATAQUE_BASICO;
        var opciones = new ArrayList<>(List.of(basico, CatalogoHabilidades.DEFENDER));
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
        var estadosAntes = objetivo.getEstados();
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
                    danio = infligirDanio(objetivo, CalculoCombate.danio(actor, objetivo, habilidad, critico, reduccionMinima));
                }
            }
            case DANIO_MAGICO -> {
                danio = infligirDanio(objetivo, CalculoCombate.danio(actor, objetivo, habilidad, false, reduccionMinima));
            }
            case CURACION -> curacion = objetivo.recibirCuracion(CalculoCombate.curacion(actor, habilidad));
            case REVIVIR -> {
                curacion = objetivo.revivir(habilidad.potencia());
                ((Enemigo) actor).registrarReanimacion();
            }
            case MARCAR -> ((Enemigo) actor).marcarObjetivo(objetivo.getId());
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
        if (interceptando && !esquivado) objetivo.retirarEstado(INTERCEPTANDO);
        int curacionActor = 0;
        if (danio > 0 && habilidad.porcentajeRoboVida() > 0) {
            curacionActor = actor.recibirCuracion(danio * habilidad.porcentajeRoboVida() / 100);
        }
        return new EventoCombate(turno, actor.getId(), tipo(actor), habilidad.id(), objetivo.getId(),
                tipo(objetivo), danio, curacion, 0, esquivado, critico, objetoRobado, curacionActor,
                EventoCombate.CambioEstados.entre(estadosAntes, objetivo.getEstados()),
                habilidad.id().equals(CatalogoHabilidades.ATAQUE_BASICO.id()) ? habilidad.nombre() : null);
    }

    private int infligirDanio(Combatiente objetivo, int potencia) {
        int danio = modoPruebas && heroes.contains(objetivo) ? 0 : Math.min(objetivo.getPvActuales(), potencia);
        objetivo.recibirDanio(danio);
        return danio;
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
                int potencia = estado == QUEMADO ? CalculoCombate.danioQuemadura(actor) : estado.danioPeriodico();
                int danio = infligirDanio(actor, potencia);
                historial.add(new EventoCombate(turno, actor.getId(), tipo(actor), estado.eventoDanio(), actor.getId(),
                        tipo(actor), danio, 0, 0, false, false, null));
                if (!actor.estaVivo()) {
                    registrarDialogoJefe(actor, false);
                    break;
                }
            }
        }
        incorporarRefuerzos(actor);
        var antesDeLasMarcas = heroes.stream().map(EstadoVisual::new).toList();
        actualizarMarcasEspectrales();
        if (!historial.isEmpty() && historial.getLast().turno() == turno) {
            registrarAjustes(historial.size() - 1, antesDeLasMarcas.stream().map(EstadoVisual::cambio).toList(), false);
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

    private void incorporarRefuerzos(Combatiente actor) {
        if (refuerzos.isEmpty() || heroes.stream().noneMatch(Combatiente::estaVivo)) return;
        boolean segundoTurnoDeAzrath = actor == azrathTrasladado
                && azrathTrasladado.getAccionesRealizadas() - accionesPreviasDeAzrath == 1;
        if (azrathTrasladado.estaVivo() && !segundoTurnoDeAzrath) return;
        enemigos.addAll(refuerzos);
        participantes.addAll(refuerzos);
        refuerzos.stream().sorted(Comparator.comparingInt(Combatiente::getVelocidadEfectiva).reversed())
                .forEachOrdered(pendientes::addLast);
        refuerzos.clear();
        historial.add(new EventoCombate(turno, actor.getId(), tipo(actor), "llegan_draconianos", actor.getId(),
                tipo(actor), 0, 0, 0, false, false, null));
    }

    private void actualizarMarcasEspectrales() {
        var marcados = new HashSet<UUID>();
        for (Combatiente rival : enemigos) {
            if (!(rival instanceof Enemigo verdugo) || verdugo.getObjetivoMarcado() == null) continue;
            UUID objetivo = verdugo.getObjetivoMarcado();
            if (verdugo.estaVivo() && heroes.stream().anyMatch(heroe -> heroe.estaVivo() && heroe.getId().equals(objetivo))) {
                marcados.add(objetivo);
            } else {
                verdugo.marcarObjetivo(null);
            }
        }

        for (Heroe heroe : heroes) {
            if (marcados.contains(heroe.getId())) {
                heroe.aplicarEstado(MARCA_ESPECTRAL, 1);
            } else {
                heroe.retirarEstado(MARCA_ESPECTRAL);
            }
        }
    }

    private void ordenarCiclo() {
        participantes.stream().filter(Combatiente::estaVivo)
                .sorted(Comparator.comparingInt(Combatiente::getVelocidadEfectiva).reversed())
                .forEachOrdered(pendientes::addLast);
    }
}
