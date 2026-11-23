package es.rpgbattlegauntlet.model;

import es.rpgbattlegauntlet.algorithm.GeneradorBotin;
import es.rpgbattlegauntlet.config.CatalogoObjetos;
import es.rpgbattlegauntlet.config.Equilibrio;
import es.rpgbattlegauntlet.enums.EstadoTemporal;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Random;

import static es.rpgbattlegauntlet.enums.Atributo.FUE;
import static es.rpgbattlegauntlet.enums.Calidad.*;
import static es.rpgbattlegauntlet.enums.Categoria.*;
import static es.rpgbattlegauntlet.enums.ClaseHeroe.*;
import static es.rpgbattlegauntlet.enums.PotenciaPergamino.PODEROSO;
import static es.rpgbattlegauntlet.enums.TipoEnemigo.*;
import static org.junit.jupiter.api.Assertions.*;

class ProgresionTest {

    @Test
    void equipoInicialAplicaBonosYSoloLosAtributosBaseCumplenRequisitos() {
        var partida = trasPrimerEncuentro();
        var guerrero = partida.getEquipo().getFirst();
        var arquero = partida.getEquipo().get(1);
        var mago = partida.getEquipo().getLast();
        assertEquals(19, guerrero.getEstadisticasBase().fuerza() + guerrero.getBonificaciones().fuerza());
        assertEquals(23, guerrero.getDefensaEfectiva());
        assertEquals(3, guerrero.getVelocidadEfectiva());
        assertEquals(3, mago.getBonificaciones().potenciaMagica());

        partida.comprar("arco_largo", null);
        var arco = objeto(partida, "arco_largo");
        assertThrows(IllegalArgumentException.class, () -> partida.equipar(arquero.getId(), arco.id()));
        assertEquals(arco, objeto(partida, "arco_largo"));
        assertThrows(IllegalArgumentException.class, () -> partida.invertir(arquero.getId(), FUE, 4));
        partida.invertir(arquero.getId(), FUE, 2);
        partida.equipar(arquero.getId(), arco.id());
        assertEquals(arco, arquero.getEquipo().get(ARMAS));
        assertEquals(1, arquero.getPuntos());
        var inicial = objeto(partida, "arco_corto");
        assertThrows(IllegalArgumentException.class, () -> partida.vender(inicial.id()));
        partida.desequipar(arquero.getId(), ARMAS);
        assertNull(arquero.habilidadArma());
        partida.equipar(guerrero.getId(), arco.id());
        assertEquals("disparo_tenso", guerrero.habilidadArma().id());
    }

    @Test
    void recompraConservaIdentidadYElCatalogoFijoNoSeRepone() {
        var partida = trasPrimerEncuentro();
        partida.comprar("proyectil", null);
        var pergamino = objeto(partida, "proyectil");
        assertThrows(IllegalArgumentException.class, () -> partida.comprar("proyectil", null));

        partida.vender(pergamino.id());
        assertEquals(2, partida.getOfertas().stream().filter(oferta -> oferta.objeto().codigo().equals("proyectil")).count());
        var segundaMano = partida.getOfertas().stream().filter(oferta -> pergamino.id().equals(oferta.objeto().id()))
                .findFirst().orElseThrow();
        assertEquals(pergamino.precioVenta() * 2, segundaMano.precio());
        partida.comprar(null, pergamino.id());
        assertSame(pergamino, objeto(partida, "proyectil"));
        assertThrows(IllegalArgumentException.class, () -> partida.comprar(null, pergamino.id()));
        assertThrows(IllegalArgumentException.class, () -> partida.comprar("proyectil", null));
    }

    @Test
    void pocionesIlimitadasConUnidadesIndependientesYConsumoEnCampamento() {
        var partida = trasPrimerEncuentro();
        var mago = partida.getEquipo().getLast();
        mago.recibirDanio(10);
        mago.consumirMagia(21);
        partida.comprar("azul", null);
        partida.comprar("azul", null);
        assertEquals(2, partida.getInventario().stream().map(Objeto::id).distinct().count());
        var primera = objeto(partida, "azul");
        partida.usarConsumible(0, mago.getId(), primera.id(), mago.getId());
        assertEquals(75, mago.getPvActuales());
        assertEquals(100, mago.getPmActuales());
        assertEquals(1, partida.getInventario().size());
        assertThrows(IllegalArgumentException.class, () -> partida.usarConsumible(0, mago.getId(), primera.id(), mago.getId()));
        assertNull(partida.getOfertas().stream().filter(oferta -> oferta.objeto().codigo().equals("azul"))
                .findFirst().orElseThrow().existencias());
        assertThrows(IllegalArgumentException.class, () -> partida.comprar("vara_avanzada", null));
    }

    @Test
    void pergaminoPideInteligenciaBaseRespetaResistenciaYNoGastaPm() {
        var partida = trasPrimerEncuentro();
        var mago = partida.getEquipo().getLast();
        partida.comprar("ventisca", null);
        var pergamino = objeto(partida, "ventisca");
        var combate = partida.avanzarRonda(1);
        var primerActor = combate.getOrdenActuacion().getFirst();
        var enemigo = combate.getEnemigos().getFirst();

        assertThrows(IllegalArgumentException.class, () -> partida.usarConsumible(combate.getTurno(),
                primerActor.getId(), pergamino.id(), null));
        assertEquals(pergamino, objeto(partida, "ventisca"));
        while (combate.getOrdenActuacion().getFirst() != mago) {
            partida.actuar(new AccionCombate(combate.getTurno(), combate.getOrdenActuacion().getFirst().getId(), "defender", null));
        }
        int turno = combate.getTurno();
        int magia = mago.getPmActuales();
        partida.usarConsumible(turno, mago.getId(), pergamino.id(), null);

        assertEquals(magia, mago.getPmActuales());
        assertEquals(4, enemigo.getPvActuales());
        assertEquals(1, combate.getConsumiblesUsados());
        assertTrue(combate.getTurno() > turno);
        assertTrue(partida.getInventario().isEmpty());
    }

    @Test
    void catalogoTieneCuatroApartadosYSoloTresPociones() {
        var ofertas = new Tienda().ofertas();
        assertEquals(4, ofertas.stream().map(oferta -> oferta.objeto().categoria()).distinct().count());
        assertEquals(10, ofertas.stream().filter(oferta -> oferta.objeto().categoria() == ARMAS).count());
        assertEquals(9, ofertas.stream().filter(oferta -> oferta.objeto().categoria() == ARMADURAS).count());
        assertEquals(3, ofertas.stream().filter(oferta -> oferta.objeto().categoria() == POCIONES).count());
        assertEquals(10, ofertas.stream().filter(oferta -> oferta.objeto().categoria() == PERGAMINOS).count());
        assertTrue(ofertas.stream().filter(oferta -> oferta.objeto().categoria() == PERGAMINOS)
                .allMatch(oferta -> Integer.valueOf(1).equals(oferta.existencias())));
    }

    @Test
    void pocionEnCombateGastaUnaAccionYRegistraMagiaRecuperada() {
        var partida = trasPrimerEncuentro();
        var mago = partida.getEquipo().getLast();
        mago.consumirMagia(55);
        partida.comprar("verde", null);
        var pocion = objeto(partida, "verde");
        var combate = partida.avanzarRonda(1);
        int turno = combate.getTurno();
        var actor = combate.getOrdenActuacion().getFirst();

        partida.usarConsumible(turno, actor.getId(), pocion.id(), mago.getId());

        assertTrue(combate.getTurno() > turno);
        assertEquals(95, mago.getPmActuales());
        assertEquals(1, combate.getConsumiblesUsados());
        assertEquals(50, combate.getHistorial().getFirst().magiaRecuperada());
        assertTrue(partida.getInventario().isEmpty());
    }

    @Test
    void curaTotalPoderosaCuraYProtegeDosTurnos() {
        var mago = new Heroe(MAGO, new EstadisticasBase(75, 100, 4, 5, 21, 14, 9, 5, 5));
        var rival = new Combatiente(new EstadisticasBase(80, 0, 5, 4, 0, 2, 1, 0, 0));
        var combate = new Combate(List.of(mago), List.of(rival), azar());
        mago.recibirDanio(60);
        Objeto pergamino = CatalogoObjetos.pergamino("cura_total", PODEROSO);

        combate.usarConsumible(1, mago.getId(), pergamino, mago.getId());

        assertEquals(75, mago.getPvActuales());
        assertEquals(35, mago.getReduccionDanio());
        assertEquals(2, mago.getEstados().get(EstadoTemporal.PROTECCION_35));
        combate.ejecutar(new AccionCombate(2, rival.getId(), "defender", null));
        combate.ejecutar(new AccionCombate(3, mago.getId(), "defender", null));
        assertEquals(1, mago.getEstados().get(EstadoTemporal.PROTECCION_35));
        combate.ejecutar(new AccionCombate(4, rival.getId(), "defender", null));
        combate.ejecutar(new AccionCombate(5, mago.getId(), "defender", null));
        assertFalse(mago.getEstados().containsKey(EstadoTemporal.PROTECCION_35));
    }

    @Test
    void tiradasSeparanProbabilidadTipoYCalidad() {
        assertTrue(GeneradorBotin.tirar(GOBLIN, azar(35)).isEmpty());
        var daga = GeneradorBotin.tirar(GOBLIN, azar(34, 44, 34)).orElseThrow();
        assertEquals("Daga mellada", daga.nombre());
        assertEquals(INFERIOR, daga.calidad());
        assertEquals(12, daga.precioVenta());
        assertEquals(24, daga.precioRecompra());
        var capucha = GeneradorBotin.tirar(GOBLIN, azar(0, 45, 35)).orElseThrow();
        assertEquals(NORMAL, capucha.calidad());
        var oscura = GeneradorBotin.tirar(CABALLERO_OSCURO, azar(74, 40, 85)).orElseThrow();
        assertEquals(SUPERIOR, oscura.calidad());
        assertEquals(-5, oscura.bonificaciones().velocidad());
        assertEquals(20, oscura.fuerzaMinima());
        var meteorito = GeneradorBotin.tirar(CHAMAN, azar(49, 80, 9, 99)).orElseThrow();
        assertEquals(PODEROSO, meteorito.potencia());
        assertEquals(200, meteorito.efecto().cantidad());
        assertEquals(25, meteorito.inteligenciaMinima());
        assertEquals(400, meteorito.precioRecompra());
        assertTrue(GeneradorBotin.tirar(DRAGON, azar()).isEmpty());
    }

    @Test
    void recompensaSeEntregaUnaVezYElBotinSoloSeRecogeUnaVez() {
        var partida = primerEncuentro(azar(99, 99, 0, 0, 35));
        var combate = partida.getCombate();
        var recompensa = partida.getRecompensa();
        assertEquals(Equilibrio.oroRonda(1, 1, 3, 0), recompensa.oro());
        assertEquals(75, recompensa.experiencia());
        assertEquals(2, partida.getEquipo().getFirst().getNivel());
        assertEquals(3, partida.getEquipo().getFirst().getPuntos());
        assertThrows(IllegalStateException.class, () -> partida.actuar(new AccionCombate(
                combate.getTurno(), partida.getEquipo().get(1).getId(), "defender", null)));
        var botin = partida.getBotin().getFirst();
        partida.recoger(botin.id());
        assertThrows(IllegalArgumentException.class, () -> partida.recoger(botin.id()));
        partida.avanzarRonda(1);
        assertEquals(recompensa.oro(), partida.getOro());
        assertEquals(1, partida.getInventario().size());
    }

    private static Partida trasPrimerEncuentro() {
        return primerEncuentro(azar());
    }

    private static Partida primerEncuentro(Random azar) {
        var partida = new Partida(azar);
        partida.seleccionarHeroes(List.of(GUERRERO, ARQUERO, MAGO));
        var combate = partida.iniciarCombate();
        var enemigo = combate.getEnemigos().getFirst();
        enemigo.recibirDanio(enemigo.getPvActuales() - 1);
        partida.actuar(new AccionCombate(combate.getTurno(), partida.getEquipo().get(1).getId(), "ataque_basico", enemigo.getId()));
        return partida;
    }

    private static Objeto objeto(Partida partida, String codigo) {
        return partida.getInventario().stream().filter(objeto -> objeto.codigo().equals(codigo)).findFirst().orElseThrow();
    }

    private static Random azar(Integer... tiradas) {
        var pendientes = new ArrayDeque<>(List.of(tiradas));
        return new Random(0) {
            @Override
            public int nextInt(int limite) {
                return pendientes.isEmpty() ? limite - 1 : pendientes.removeFirst();
            }
        };
    }
}
