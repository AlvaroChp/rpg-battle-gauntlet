package es.rpgbattlegauntlet.model;

import es.rpgbattlegauntlet.algorithm.InteligenciaEnemiga;
import es.rpgbattlegauntlet.config.CatalogoEnemigos;
import es.rpgbattlegauntlet.config.CatalogoHeroes;
import es.rpgbattlegauntlet.enums.EstadoTemporal;
import es.rpgbattlegauntlet.enums.TipoEnemigo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.random.RandomGenerator;

import static es.rpgbattlegauntlet.enums.ClaseHeroe.*;
import static es.rpgbattlegauntlet.enums.TipoEnemigo.*;
import static org.junit.jupiter.api.Assertions.*;

class EnemigoTest {

    @ParameterizedTest
    @CsvSource({
            "GOBLIN,65,0,10,5,0,4,11,5,10",
            "ESQUELETO,90,0,12,10,0,5,7,5,5",
            "CHAMAN,70,80,5,5,14,12,9,5,5",
            "OGRO,220,0,20,14,0,8,4,10,0",
            "CABALLERO_OSCURO,240,40,20,18,10,15,10,10,5",
            "DRAGON,650,120,27,20,22,18,12,15,5"
    })
    void conservaEstadisticasFijas(TipoEnemigo tipo, int pv, int pm, int fuerza, int defensa,
                                  int inteligencia, int resistencia, int velocidad, int critico, int esquiva) {
        var enemigo = CatalogoEnemigos.crear(tipo);

        assertEquals(new EstadisticasBase(pv, pm, fuerza, defensa, inteligencia, resistencia, velocidad, critico, esquiva),
                enemigo.getEstadisticasBase());
        assertEquals(pv, enemigo.getPvActuales());
        assertEquals(pm, enemigo.getPmActuales());
        assertNotEquals(enemigo.getId(), CatalogoEnemigos.crear(tipo).getId());
    }

    @Test
    void goblinActuaAutomaticamenteYElAtaqueDobleUsaUnSoloTurno() {
        var guerrero = CatalogoHeroes.crearHeroe(GUERRERO);
        var goblin = CatalogoEnemigos.crear(GOBLIN);
        var combate = new Combate(List.of(guerrero), List.of(goblin), azar());

        assertEquals("cuchillada", combate.getHistorial().getFirst().habilidad());
        defender(combate, guerrero);

        var golpes = combate.getHistorial().stream().filter(evento -> evento.habilidad().equals("ataque_doble")).toList();
        assertEquals(2, golpes.size());
        assertEquals(golpes.getFirst().turno(), golpes.getLast().turno());
        assertEquals(guerrero, combate.getOrdenActuacion().getFirst());
    }

    @Test
    void esqueletoSeProtegeYOgroAlcancaAlEquipoEnSuSegundoTurno() {
        var esqueleto = CatalogoEnemigos.crear(ESQUELETO);
        var ogro = CatalogoEnemigos.crear(OGRO);
        var guerrero = CatalogoHeroes.crearHeroe(GUERRERO);
        var arquero = CatalogoHeroes.crearHeroe(ARQUERO);
        var combate = new Combate(List.of(guerrero, arquero), List.of(esqueleto, ogro), azar());

        defender(combate, arquero);
        defender(combate, guerrero);
        defender(combate, arquero);
        defender(combate, guerrero);

        assertEquals(45, esqueleto.getReduccionDanio());
        var barrido = combate.getHistorial().stream().filter(evento -> evento.habilidad().equals("barrido")).toList();
        assertEquals(List.of(guerrero.getId(), arquero.getId()), barrido.stream().map(EventoCombate::objetivo).toList());
    }

    @Test
    void chamanCuraAlAliadoConMenorProporcionDeVidaYConsumeMagia() {
        var chaman = CatalogoEnemigos.crear(CHAMAN);
        var ogro = CatalogoEnemigos.crear(OGRO);
        var esqueleto = CatalogoEnemigos.crear(ESQUELETO);
        var guerrero = CatalogoHeroes.crearHeroe(GUERRERO);
        ogro.recibirDanio(110);
        esqueleto.recibirDanio(55);

        var combate = new Combate(List.of(guerrero), List.of(chaman, ogro, esqueleto), azar());

        var evento = combate.getHistorial().getFirst();
        assertEquals("curacion_oscura", evento.habilidad());
        assertEquals(esqueleto.getId(), evento.objetivo());
        assertEquals(44, evento.curacion());
        assertEquals(66, chaman.getPmActuales());
        assertEquals(110, ogro.getPvActuales());
    }

    @Test
    void chamanSinMagiaUsaAtaqueBasico() {
        var chaman = CatalogoEnemigos.crear(CHAMAN);
        var heroe = CatalogoHeroes.crearHeroe(GUERRERO);
        chaman.consumirMagia(80);

        var combate = new Combate(List.of(heroe), List.of(chaman), azar());

        assertEquals("ataque_basico", combate.getHistorial().getFirst().habilidad());
        assertEquals(0, chaman.getPmActuales());
    }

    @Test
    void caballeroDebilitaDefensaYContraatacaUnaVezTrasPrepararse() {
        var caballero = CatalogoEnemigos.crear(CABALLERO_OSCURO);
        var heroe = CatalogoHeroes.crearHeroe(GUERRERO);
        var combate = new Combate(List.of(heroe), List.of(caballero), azar());

        assertEquals(10, heroe.getDefensaEfectiva());
        defender(combate, heroe);
        assertEquals(55, caballero.getReduccionDanio());
        defender(combate, heroe);
        assertTrue(caballero.getEstados().containsKey(EstadoTemporal.CONTRAATAQUE_LISTO));

        combate.ejecutar(new AccionCombate(combate.getTurno(), heroe.getId(), "ataque_basico", caballero.getId()));

        var reacciones = combate.getHistorial().stream().filter(evento -> evento.habilidad().equals("contraataque")).toList();
        assertEquals(1, reacciones.size());
        assertEquals(heroe.getId(), reacciones.getFirst().objetivo());
        assertFalse(caballero.getEstados().containsKey(EstadoTemporal.CONTRAATAQUE_LISTO));
    }

    @Test
    void dragonCambiaDeFasePorVidaYRecurreAGarraSinPm() {
        var dragon = CatalogoEnemigos.crear(DRAGON);
        var heroes = List.of(CatalogoHeroes.crearHeroe(GUERRERO));
        List<Combatiente> aliados = List.of(dragon);

        dragon.registrarAccion();
        assertEquals(CatalogoEnemigos.GARRA, InteligenciaEnemiga.decidir(dragon, aliados, heroes, azar()).habilidad());
        dragon.recibirDanio(195);
        assertEquals(CatalogoEnemigos.ALIENTO, InteligenciaEnemiga.decidir(dragon, aliados, heroes, azar()).habilidad());
        dragon.recibirDanio(228);
        assertEquals(CatalogoEnemigos.RUGIDO, InteligenciaEnemiga.decidir(dragon, aliados, heroes, azar()).habilidad());
        dragon.registrarAccion();
        assertEquals(CatalogoEnemigos.ALIENTO, InteligenciaEnemiga.decidir(dragon, aliados, heroes, azar()).habilidad());
        dragon.consumirMagia(120);
        assertEquals(CatalogoEnemigos.GARRA, InteligenciaEnemiga.decidir(dragon, aliados, heroes, azar()).habilidad());
    }

    @Test
    void dragonEjecutaAlientoContraTodosLosHeroesVivos() {
        var dragon = CatalogoEnemigos.crear(DRAGON);
        var guerrero = CatalogoHeroes.crearHeroe(GUERRERO);
        var sanador = CatalogoHeroes.crearHeroe(SANADOR);
        dragon.recibirDanio(195);
        var combate = new Combate(List.of(guerrero, sanador), List.of(dragon), azar());

        defender(combate, sanador);
        defender(combate, guerrero);

        var aliento = combate.getHistorial().stream().filter(evento -> evento.habilidad().equals("aliento_fuego")).toList();
        assertEquals(List.of(guerrero.getId(), sanador.getId()), aliento.stream().map(EventoCombate::objetivo).toList());
        assertTrue(aliento.stream().allMatch(evento -> evento.danio() > 0));
        assertTrue(aliento.stream().noneMatch(evento -> evento.esquivado() || evento.critico()));
        assertEquals(100, dragon.getPmActuales());
    }

    @Test
    void ataquesIndividualesSorteanEntreVivosYSoloPriorizanLaProporcionDeVidaEnUnaDeCadaCuatroTiradas() {
        var goblin = CatalogoEnemigos.crear(GOBLIN);
        var guerrero = CatalogoHeroes.crearHeroe(GUERRERO);
        var mago = CatalogoHeroes.crearHeroe(MAGO);
        var arquero = CatalogoHeroes.crearHeroe(ARQUERO);
        guerrero.recibirDanio(60);
        var heroes = List.of(guerrero, mago, arquero);

        int ataquesAlGuerrero = 0;
        for (int tirada = 0; tirada < 100; tirada++) {
            for (int indice = 0; indice < heroes.size(); indice++) {
                var objetivo = InteligenciaEnemiga.decidir(goblin, List.of(goblin), heroes, azarObjetivo(tirada, indice)).objetivo();
                assertSame(tirada < 25 ? guerrero : heroes.get(indice), objetivo);
                if (objetivo == guerrero) ataquesAlGuerrero++;
            }
        }
        assertEquals(150, ataquesAlGuerrero);

        guerrero.recibirCuracion(60);
        assertSame(arquero, InteligenciaEnemiga.decidir(goblin, List.of(goblin), heroes, azarObjetivo(0, 2)).objetivo());
        guerrero.recibirDanio(150);
        assertSame(mago, InteligenciaEnemiga.decidir(goblin, List.of(goblin), heroes, azarObjetivo(0, 0)).objetivo());
    }

    private static RandomGenerator azarObjetivo(int tirada, int indice) {
        return new java.util.Random(0) {
            @Override
            public int nextInt(int limite) {
                return limite == 100 ? tirada : indice;
            }
        };
    }

    @Test
    void caballeroReviveAUnAliadoConSuUnicaPocion() {
        var picaro = picaroParaProbarRevitalizacion();
        var herido = CatalogoEnemigos.crear(CABALLERO_OSCURO);
        var aliado = CatalogoEnemigos.crear(CABALLERO_OSCURO);
        herido.recibirDanio(239);
        var combate = new Combate(List.of(picaro), List.of(herido, aliado), azar());

        combate.ejecutar(new AccionCombate(combate.getTurno(), picaro.getId(), "ataque_basico", herido.getId()));
        assertEquals(60, herido.getPvActuales());
        assertFalse(aliado.tienePocionRevitalizacion());
        assertTrue(herido.tienePocionRevitalizacion());
        var resurreccion = combate.getHistorial().getLast();
        assertEquals(aliado.getId(), resurreccion.actor());
        assertEquals(herido.getId(), resurreccion.objetivo());
        assertEquals(60, resurreccion.curacion());
        assertEquals(0, combate.getConsumiblesUsados());

        combate.ejecutar(new AccionCombate(combate.getTurno(), picaro.getId(), "ataque_basico", herido.getId()));
        assertEquals(0, herido.getPvActuales());
    }

    @Test
    void robarLaPocionImpideRevitalizarAlCompaneroCaido() {
        var picaro = picaroParaProbarRevitalizacion();
        var herido = CatalogoEnemigos.crear(CABALLERO_OSCURO);
        var aliado = CatalogoEnemigos.crear(CABALLERO_OSCURO);
        herido.recibirDanio(239);
        var combate = new Combate(List.of(picaro), List.of(herido, aliado), azarObjetivo(69, 0));

        var pocion = combate.ejecutar(new AccionCombate(combate.getTurno(), picaro.getId(), "robar", aliado.getId()));
        assertEquals("revitalizacion", pocion.codigo());
        assertFalse(aliado.tienePocionRevitalizacion());
        assertTrue(herido.tienePocionRevitalizacion());

        combate.ejecutar(new AccionCombate(combate.getTurno(), picaro.getId(), "ataque_basico", herido.getId()));
        assertEquals(0, herido.getPvActuales());
        assertTrue(combate.getHistorial().stream().noneMatch(evento -> evento.curacion() > 0));
    }

    private static Heroe picaroParaProbarRevitalizacion() {
        return new Heroe(PICARO, new EstadisticasBase(350, 25, 50, 6, 4, 7, 17, 20, 15, 25));
    }

    private static void defender(Combate combate, Heroe heroe) {
        combate.ejecutar(new AccionCombate(combate.getTurno(), heroe.getId(), "defender", null));
    }

    private static RandomGenerator azar() {
        return new RandomGenerator() {
            @Override
            public int nextInt(int limite) {
                return limite - 1;
            }

            @Override
            public long nextLong() {
                throw new AssertionError("El combate usa tiradas porcentuales");
            }
        };
    }
}
