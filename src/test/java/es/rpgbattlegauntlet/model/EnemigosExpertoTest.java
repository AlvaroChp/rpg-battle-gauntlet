package es.rpgbattlegauntlet.model;

import es.rpgbattlegauntlet.algorithm.GeneradorBotin;
import es.rpgbattlegauntlet.config.CatalogoEnemigos;
import es.rpgbattlegauntlet.config.CatalogoObjetos;
import es.rpgbattlegauntlet.enums.Dificultad;
import es.rpgbattlegauntlet.enums.TipoEnemigo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Random;

import static es.rpgbattlegauntlet.enums.Calidad.NORMAL;
import static es.rpgbattlegauntlet.enums.ClaseHeroe.*;
import static es.rpgbattlegauntlet.enums.RanuraEquipo.*;
import static es.rpgbattlegauntlet.enums.EstadoTemporal.CARGA_ONDA_RUNICA;
import static es.rpgbattlegauntlet.enums.EstadoTemporal.MARCA_ESPECTRAL;
import static es.rpgbattlegauntlet.enums.TipoEnemigo.*;
import static org.junit.jupiter.api.Assertions.*;

class EnemigosExpertoTest {

    @ParameterizedTest
    @CsvSource({"NIGROMANTE,125,15", "GOLEM_RUNICO,280,6", "VERDUGO_ESPECTRAL,185,12"})
    void respetaElPerfilYLosModificadoresDeExperto(TipoEnemigo tipo, int pvBase, int resistencia) {
        var enemigo = CatalogoEnemigos.crear(tipo, Dificultad.EXPERTO);

        assertEquals(Dificultad.EXPERTO.vidaEnemiga(pvBase), enemigo.getPvActuales());
        assertEquals(resistencia, enemigo.getResistenciaEfectiva());
        assertEquals(2.0, enemigo.getMultiplicadorDanio());
    }

    @Test
    void nigromanteGasta18PcEnRevivirYSoloPuedeHacerloUnaVez() {
        var nigromante = CatalogoEnemigos.crear(NIGROMANTE);
        var esqueleto = CatalogoEnemigos.crear(ESQUELETO);
        esqueleto.recibirDanio(esqueleto.getPvActuales());
        var heroe = heroeRapido();
        var combate = new Combate(List.of(heroe), List.of(nigromante, esqueleto), azar());

        defender(combate, heroe);
        var reanimacion = combate.getHistorial().getLast();
        assertEquals("reanimar", reanimacion.habilidad());
        assertEquals(esqueleto.getId(), reanimacion.objetivo());
        assertEquals(22, esqueleto.getPvActuales());
        assertEquals(40, nigromante.getPmActuales());

        combate.ejecutar(new AccionCombate(combate.getTurno(), heroe.getId(), "ataque_basico", esqueleto.getId()));

        assertFalse(esqueleto.estaVivo());
        assertEquals("descarga_funebre", combate.getHistorial().getLast().habilidad());
        assertEquals(1, combate.getHistorial().stream().filter(evento -> evento.habilidad().equals("reanimar")).count());
    }

    @Test
    void golemAnunciaLaOndaAntesDeGolpearATodoElEquipo() {
        var golem = CatalogoEnemigos.crear(GOLEM_RUNICO);
        var primero = heroeRapido();
        var segundo = heroeRapido();
        var combate = new Combate(List.of(primero, segundo), List.of(golem), azar());

        defender(combate, primero);
        defender(combate, segundo);
        assertEquals("preparar_onda_runica", combate.getHistorial().getLast().habilidad());
        assertTrue(golem.tieneEstado(CARGA_ONDA_RUNICA));

        defender(combate, primero);
        defender(combate, segundo);
        var onda = combate.getHistorial().stream().filter(evento -> evento.habilidad().equals("onda_runica")).toList();

        assertEquals(List.of(primero.getId(), segundo.getId()), onda.stream().map(EventoCombate::objetivo).toList());
        assertFalse(golem.tieneEstado(CARGA_ONDA_RUNICA));
    }

    @Test
    void verdugoEjecutaSentenciaContraElMarcadoYDespuesBarreAlGrupo() {
        var verdugo = CatalogoEnemigos.crear(VERDUGO_ESPECTRAL);
        var primero = heroeRapido();
        var segundo = heroeRapido();
        var combate = new Combate(List.of(primero, segundo), List.of(verdugo), azar());

        defender(combate, primero);
        defender(combate, segundo);
        defender(combate, primero);
        defender(combate, segundo);
        assertEquals(segundo.getId(), verdugo.getObjetivoMarcado());
        assertTrue(segundo.tieneEstado(MARCA_ESPECTRAL));
        assertFalse(primero.tieneEstado(MARCA_ESPECTRAL));

        defender(combate, primero);
        assertTrue(segundo.tieneEstado(MARCA_ESPECTRAL));
        defender(combate, segundo);

        var sentencia = combate.getHistorial().getLast();
        assertEquals("sentencia_espectral", sentencia.habilidad());
        assertEquals(segundo.getId(), sentencia.objetivo());
        assertEquals(30, sentencia.danio());
        assertFalse(segundo.tieneEstado(MARCA_ESPECTRAL));
        assertNull(verdugo.getObjetivoMarcado());

        defender(combate, primero);
        defender(combate, segundo);
        var barrido = combate.getHistorial().stream().filter(evento -> evento.habilidad().equals("barrido_espectral")).toList();
        assertEquals(List.of(primero.getId(), segundo.getId()), barrido.stream().map(EventoCombate::objetivo).toList());
        assertTrue(barrido.stream().allMatch(evento -> evento.danio() == 14));

        defender(combate, primero);
        defender(combate, segundo);
        assertEquals("tajo_espectral", combate.getHistorial().getLast().habilidad());
    }

    @ParameterizedTest
    @CsvSource({
            "NIGROMANTE,60,34,vara_osario", "NIGROMANTE,60,35,capucha_funeraria",
            "NIGROMANTE,60,59,capucha_funeraria", "NIGROMANTE,60,60,daga_ritual",
            "NIGROMANTE,60,69,daga_ritual", "NIGROMANTE,60,70,revitalizacion", "GOLEM_RUNICO,70,39,guantes_runicos",
            "GOLEM_RUNICO,70,40,coraza_runica", "GOLEM_RUNICO,70,75,proteccion",
            "VERDUGO_ESPECTRAL,65,39,hacha_espectral", "VERDUGO_ESPECTRAL,65,40,yelmo_espectral",
            "VERDUGO_ESPECTRAL,65,75,cura_mayor"
    })
    void botinRespetaElRepartoYTambienSePuedeRobar(TipoEnemigo tipo, int probabilidad, int tirada, String codigo) {
        assertTrue(GeneradorBotin.tirar(tipo, azar(probabilidad)).isEmpty());
        var botin = GeneradorBotin.tirar(tipo, azar(probabilidad - 1, tirada, 35)).orElseThrow();
        var robado = GeneradorBotin.elegirBotinRobado(tipo, azar(tirada, 35));

        assertEquals(codigo, botin.codigo());
        assertEquals(codigo, robado.codigo());
        assertNotEquals(botin.id(), robado.id());
        var catalogo = CatalogoObjetos.tienda().stream().filter(objeto -> objeto.codigo().equals(codigo)).findFirst();
        if (botin.ranura() == null) {
            assertNull(botin.calidad());
            assertEquals(catalogo.orElseThrow().efecto(), botin.efecto());
        } else {
            assertEquals(NORMAL, botin.calidad());
            assertTrue(catalogo.isEmpty());
        }
    }

    @Test
    void armasNuevasSonDeDosManosYConservanLaIaDeLosEnemigos() {
        var vara = CatalogoObjetos.equipo("vara_osario", NORMAL, false);
        assertTrue(vara.dosManos());
        assertEquals(List.of(MAGO, SANADOR), vara.getClasesCompatibles());
        assertEquals(18, vara.inteligenciaMinima());
        assertEquals(10, vara.bonificaciones().inteligencia());
        assertEquals(7, vara.habilidad().costePm());
        assertTrue(GeneradorBotin.habilidadesDeArmas(NIGROMANTE).isEmpty());

        var hacha = CatalogoObjetos.equipo("hacha_espectral", NORMAL, false);
        assertTrue(hacha.dosManos());
        assertEquals(List.of(GUERRERO, PICARO), hacha.getClasesCompatibles());
        assertEquals(18, hacha.fuerzaMinima());
        assertEquals(11, hacha.bonificaciones().fuerza());
        assertEquals(6, hacha.habilidad().costePm());
        assertEquals(25, hacha.habilidad().penetracion());
        assertTrue(GeneradorBotin.habilidadesDeArmas(VERDUGO_ESPECTRAL).isEmpty());
    }

    @Test
    void proteccionesNuevasOcupanLaRanuraAcordada() {
        assertEquals(CABEZA, CatalogoObjetos.equipo("capucha_funeraria", NORMAL, false).ranura());
        assertEquals(GUANTES, CatalogoObjetos.equipo("guantes_runicos", NORMAL, false).ranura());
        assertEquals(ARMADURAS, CatalogoObjetos.equipo("coraza_runica", NORMAL, false).ranura());
        assertEquals(CABEZA, CatalogoObjetos.equipo("yelmo_espectral", NORMAL, false).ranura());
    }

    private static Heroe heroeRapido() {
        return new Heroe(GUERRERO, new EstadisticasBase(200, 30, 25, 5, 5, 10, 15, 0, 0));
    }

    private static void defender(Combate combate, Heroe heroe) {
        combate.ejecutar(new AccionCombate(combate.getTurno(), heroe.getId(), "defender", null));
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
