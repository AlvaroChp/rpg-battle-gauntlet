package es.rpgbattlegauntlet.model;

import es.rpgbattlegauntlet.algorithm.GeneradorBotin;
import es.rpgbattlegauntlet.config.CatalogoEncuentros;
import es.rpgbattlegauntlet.config.CatalogoHeroes;
import es.rpgbattlegauntlet.config.CatalogoObjetos;
import es.rpgbattlegauntlet.config.Equilibrio;
import es.rpgbattlegauntlet.enums.Atributo;
import es.rpgbattlegauntlet.enums.Categoria;
import es.rpgbattlegauntlet.enums.RanuraEquipo;
import es.rpgbattlegauntlet.enums.ClaseHeroe;
import es.rpgbattlegauntlet.enums.Dificultad;
import es.rpgbattlegauntlet.enums.EstadoPartida;
import es.rpgbattlegauntlet.enums.TipoConsumible;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.random.RandomGenerator;

public final class Partida {

    private static final int TAMANO_EQUIPO = 3;

    private final UUID id = UUID.randomUUID();
    private final RandomGenerator azar;
    private final Map<UUID, Objeto> inventario = new LinkedHashMap<>();
    private final Map<UUID, Objeto> botin = new LinkedHashMap<>();
    private final Tienda tienda = new Tienda();
    private final List<EventoCombate> historialAnterior = new ArrayList<>();
    private Recompensa recompensa;
    private int oro;
    private int ronda;
    private List<Heroe> equipo = List.of();
    private Combate combate;
    private Dificultad dificultad = Dificultad.FACIL;

    public Partida() {
        this(RandomGenerator.getDefault());
    }

    public Partida(RandomGenerator azar) {
        this.azar = Objects.requireNonNull(azar, "La fuente de azar es obligatoria");
    }

    public UUID getId() {
        return id;
    }

    public synchronized EstadoPartida getEstado() {
        if (combate == null) {
            return EstadoPartida.SELECCION_PERSONAJES;
        }
        return switch (combate.getResultado()) {
            case EN_CURSO -> EstadoPartida.COMBATE;
            case GANADO -> ronda == getTotalRondas()
                    ? EstadoPartida.VICTORIA : EstadoPartida.ENTRE_COMBATES;
            case PERDIDO -> EstadoPartida.DERROTA;
        };
    }

    public synchronized int getRonda() {
        return ronda;
    }

    public synchronized int getTotalRondas() {
        return CatalogoEncuentros.totalRondas(dificultad);
    }

    public synchronized Combate getCombate() {
        return combate;
    }

    public synchronized Dificultad getDificultad() {
        return dificultad;
    }

    public synchronized Combate iniciarCombate() {
        return iniciarCombate(Dificultad.FACIL);
    }

    public synchronized Combate iniciarCombate(Dificultad modo) {
        if (getEstado() != EstadoPartida.SELECCION_PERSONAJES || equipo.size() != TAMANO_EQUIPO) {
            throw new IllegalStateException("Solo puedes iniciar el combate con un equipo completo en selección");
        }
        Combate inicial = abrirRonda(1, 1, Objects.requireNonNull(modo));
        dificultad = modo;
        return inicial;
    }

    public synchronized Combate avanzarRonda(int rondaCompletada) {
        if (getEstado() != EstadoPartida.ENTRE_COMBATES || rondaCompletada != ronda) {
            throw new IllegalStateException("Solo puedes avanzar desde la ronda actual tras ganarla");
        }
        liquidarVictoria();
        botin.clear();
        historialAnterior.addAll(combate.getHistorial());
        return abrirRonda(ronda + 1, combate.getTurno() + 1, dificultad);
    }

    private Combate abrirRonda(int numero, int primerTurno, Dificultad modo) {
        var siguienteCombate = new Combate(equipo, CatalogoEncuentros.crearEnemigos(numero, modo), azar, primerTurno);
        combate = siguienteCombate;
        ronda = numero;
        return combate;
    }

    public synchronized List<EventoCombate> getHistorial() {
        var eventos = new ArrayList<>(historialAnterior);
        if (combate != null) {
            eventos.addAll(combate.getHistorial());
        }
        return List.copyOf(eventos);
    }

    public synchronized boolean tieneOroIlimitado() {
        return dificultad == Dificultad.PRUEBAS;
    }

    public synchronized int getOro() {
        return oro;
    }

    public synchronized List<Objeto> getInventario() {
        return List.copyOf(inventario.values());
    }

    public synchronized List<Objeto> getBotin() {
        return List.copyOf(botin.values());
    }

    public synchronized List<Oferta> getOfertas() {
        return tienda.ofertas(tieneOroIlimitado());
    }

    public synchronized Recompensa getRecompensa() {
        return getEstado() == EstadoPartida.ENTRE_COMBATES ? recompensa : null;
    }

    public synchronized void actuar(AccionCombate accion) {
        exigirEstado(EstadoPartida.COMBATE);
        Objeto robado = combate.ejecutar(accion);
        if (robado != null) inventario.put(robado.id(), robado);
        liquidarVictoria();
    }

    public synchronized void recoger(UUID idObjeto) {
        exigirEstado(EstadoPartida.ENTRE_COMBATES);
        Objeto objeto = botin.remove(idObjeto);
        if (objeto == null) {
            throw new IllegalArgumentException("Ese botín ya no está disponible");
        }
        inventario.put(idObjeto, objeto);
    }

    public synchronized void equipar(UUID idHeroe, UUID idObjeto) {
        exigirEstado(EstadoPartida.ENTRE_COMBATES);
        Objeto objeto = objetoEnInventario(idObjeto);
        List<Objeto> retirados = heroe(idHeroe).equipar(objeto);
        inventario.remove(idObjeto);
        retirados.forEach(pieza -> inventario.put(pieza.id(), pieza));
    }

    public synchronized void desequipar(UUID idHeroe, RanuraEquipo ranura) {
        exigirEstado(EstadoPartida.ENTRE_COMBATES);
        if (ranura == null) {
            throw new IllegalArgumentException("Ranura de equipo inválida");
        }
        Objeto objeto = heroe(idHeroe).desequipar(ranura);
        inventario.put(objeto.id(), objeto);
    }

    public synchronized void comprar(String codigo, UUID unidad) {
        exigirEstado(EstadoPartida.ENTRE_COMBATES);
        boolean oroIlimitado = tieneOroIlimitado();
        Objeto objeto = tienda.comprar(codigo, unidad, oro, oroIlimitado);
        if (!oroIlimitado) {
            oro -= unidad == null ? objeto.precioCatalogo() : objeto.precioRecompra();
        }
        inventario.put(objeto.id(), objeto);
    }

    public synchronized void vender(UUID idObjeto) {
        exigirEstado(EstadoPartida.ENTRE_COMBATES);
        Objeto objeto = objetoEnInventario(idObjeto);
        inventario.remove(idObjeto);
        if (!tieneOroIlimitado()) {
            oro += objeto.precioVenta();
        }
        tienda.recibir(objeto);
    }

    public synchronized void usarConsumible(int turno, UUID actor, UUID idObjeto, UUID objetivo) {
        EstadoPartida estado = getEstado();
        if (estado != EstadoPartida.COMBATE && estado != EstadoPartida.ENTRE_COMBATES) {
            throw new IllegalStateException("No se pueden usar consumibles en este estado");
        }
        Objeto objeto = objetoEnInventario(idObjeto);
        if (estado == EstadoPartida.COMBATE) {
            combate.usarConsumible(turno, actor, objeto, objetivo);
        } else {
            if (objeto.categoria() != Categoria.POCIONES) {
                throw new IllegalArgumentException("Entre combates solo se pueden usar pociones");
            }
            Heroe usuario = heroe(actor);
            Heroe destinatario = heroe(objetivo);
            if (!usuario.estaVivo()) {
                throw new IllegalArgumentException("Un héroe vivo debe administrar la poción");
            }
            if (objeto.efecto().tipo() == TipoConsumible.REVITALIZACION) {
                destinatario.revivir(objeto.efecto().cantidad());
            } else {
                if (!destinatario.estaVivo()) {
                    throw new IllegalArgumentException("La poción requiere un destinatario vivo");
                }
                destinatario.recibirCuracion(objeto.efecto().cantidad());
                destinatario.recuperarMagia(objeto.efecto().magia());
            }
        }
        inventario.remove(idObjeto);
        liquidarVictoria();
    }

    public synchronized void invertir(UUID idHeroe, Atributo atributo, int cantidad) {
        exigirEstado(EstadoPartida.ENTRE_COMBATES);
        heroe(idHeroe).invertir(atributo, cantidad);
    }

    private void liquidarVictoria() {
        if (getEstado() != EstadoPartida.ENTRE_COMBATES || (recompensa != null && recompensa.ronda() == ronda)) {
            return;
        }
        var supervivientes = equipo.stream().filter(Heroe::estaVivo).toList();
        int turnos = combate.getCantidadTurnos();
        int consumibles = combate.getConsumiblesUsados();
        int experiencia = Equilibrio.experienciaRonda(ronda);
        int monedas = tieneOroIlimitado() ? 0 : Equilibrio.oroRonda(ronda, turnos, supervivientes.size(), consumibles);
        recompensa = new Recompensa(ronda, monedas, experiencia, turnos, supervivientes.size(), consumibles);
        oro += monedas;
        supervivientes.forEach(heroe -> heroe.ganarExperiencia(experiencia));
        for (Combatiente enemigo : combate.getEnemigos()) {
            if (!combate.fueRobado(enemigo.getId())) {
                GeneradorBotin.tirar(((Enemigo) enemigo).getTipo(), azar).ifPresent(objeto -> botin.put(objeto.id(), objeto));
            }
        }
    }

    private Heroe heroe(UUID idHeroe) {
        return equipo.stream().filter(heroe -> heroe.getId().equals(idHeroe)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("El héroe no existe"));
    }

    private Objeto objetoEnInventario(UUID idObjeto) {
        Objeto objeto = inventario.get(idObjeto);
        if (objeto == null) {
            throw new IllegalArgumentException("El objeto no está en el inventario");
        }
        return objeto;
    }

    private void exigirEstado(EstadoPartida esperado) {
        if (getEstado() != esperado) {
            throw new IllegalStateException("La acción necesita el estado " + esperado);
        }
    }

    public synchronized List<Heroe> getEquipo() {
        return equipo;
    }

    public synchronized void seleccionarHeroes(List<ClaseHeroe> clases) {
        if (combate != null) {
            throw new IllegalStateException("La selección ya ha terminado");
        }
        if (clases == null || clases.size() != TAMANO_EQUIPO) {
            throw new IllegalArgumentException("Debes seleccionar exactamente tres héroes");
        }

        var clasesDistintas = new HashSet<>(clases);
        if (clasesDistintas.contains(null) || clasesDistintas.size() != TAMANO_EQUIPO) {
            throw new IllegalArgumentException("Debes seleccionar tres clases diferentes");
        }

        if (equipo.stream().map(Heroe::getClase).toList().equals(clases)) {
            return;
        }

        equipo = clases.stream().map(CatalogoHeroes::crearHeroe).toList();
        equipo.forEach(heroe -> CatalogoObjetos.inicial(heroe.getClase()).forEach(heroe::equipar));
    }
}
