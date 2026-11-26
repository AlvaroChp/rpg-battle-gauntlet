package es.rpgbattlegauntlet.model;

import es.rpgbattlegauntlet.config.CatalogoHeroes;
import es.rpgbattlegauntlet.config.CatalogoObjetos;
import es.rpgbattlegauntlet.enums.Calidad;
import es.rpgbattlegauntlet.enums.ClaseHeroe;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Random;

import static es.rpgbattlegauntlet.enums.Atributo.FUE;
import static es.rpgbattlegauntlet.enums.Calidad.NORMAL;
import static es.rpgbattlegauntlet.enums.ClaseHeroe.*;
import static es.rpgbattlegauntlet.enums.RanuraEquipo.*;
import static org.junit.jupiter.api.Assertions.*;

class EquipamientoTest {

    @Test
    void seisRanurasSumanBonosYSeCambianPorSeparado() {
        var guerrero = heroe(GUERRERO);
        var torso = equipar(guerrero, "pesada");
        var guantes = equipar(guerrero, "guantes_reforzados");
        equipar(guerrero, "espada_basica");
        equipar(guerrero, "casco");
        equipar(guerrero, "botas");
        equipar(guerrero, "escudo");

        assertEquals(6, guerrero.getEquipo().size());
        assertEquals(20, guerrero.getBonificaciones().defensa());
        assertEquals(-1, guerrero.getBonificaciones().velocidad());
        assertSame(guantes, guerrero.desequipar(GUANTES));
        assertSame(torso, guerrero.getEquipo().get(ARMADURAS));
        assertEquals(18, guerrero.getBonificaciones().defensa());
    }

    @Test
    void cascoReforzadoSeEquipaEnCabezaAlLlegarA16DeFuerzaBase() {
        var guerrero = CatalogoHeroes.crearHeroe(GUERRERO);
        equipar(guerrero, "espada_basica");
        var torso = equipar(guerrero, "pesada_basica");
        var casco = CatalogoObjetos.equipo("casco", NORMAL, false);

        assertEquals(CABEZA, casco.ranura());
        assertEquals(15, guerrero.getEstadisticasBase().fuerza());
        assertThrows(IllegalArgumentException.class, () -> guerrero.equipar(casco));

        guerrero.ganarExperiencia(75);
        guerrero.invertir(FUE, 1);
        guerrero.equipar(casco);
        assertSame(casco, guerrero.getEquipo().get(CABEZA));
        assertSame(torso, guerrero.getEquipo().get(ARMADURAS));
    }

    @Test
    void armaduraPesadaRequiere16DeFuerzaBaseEnTodasLasCalidades() {
        var guerrero = CatalogoHeroes.crearHeroe(GUERRERO);
        var inicial = equipar(guerrero, "pesada_basica");
        for (var calidad : Calidad.values()) {
            var pesada = CatalogoObjetos.equipo("pesada", calidad, false);
            assertEquals(16, pesada.fuerzaMinima());
            assertThrows(IllegalArgumentException.class, () -> guerrero.equipar(pesada));
        }
        guerrero.ganarExperiencia(75);
        guerrero.invertir(FUE, 1);
        var pesada = CatalogoObjetos.equipo("pesada", NORMAL, false);

        assertEquals(List.of(inicial), guerrero.equipar(pesada));
        assertSame(pesada, guerrero.getEquipo().get(ARMADURAS));
    }

    @Test
    void magoEmpiezaConSombreroYTunica() {
        var partida = new Partida();
        partida.seleccionarHeroes(List.of(MAGO, SANADOR, GUERRERO));
        var mago = partida.getEquipo().getFirst();
        var sombrero = mago.getEquipo().get(CABEZA);

        assertEquals("sombrero_mago", sombrero.codigo());
        assertEquals(12, sombrero.inteligenciaMinima());
        assertEquals("tunica_aprendiz", mago.getEquipo().get(ARMADURAS).codigo());
        assertEquals(5, mago.getBonificaciones().potenciaMagica());
        assertEquals(1, mago.getBonificaciones().resistencia());
    }

    @ParameterizedTest
    @CsvSource({"GUERRERO,espada_basica,true", "PICARO,daga,true", "MAGO,vara_magica,false",
            "SANADOR,vara_aprendiz,false", "ARQUERO,arco_largo,false"})
    void escudoRespetaClaseYArma(ClaseHeroe clase, String arma, boolean permitido) {
        var heroe = heroe(clase);
        equipar(heroe, arma);
        var escudo = CatalogoObjetos.equipo("escudo", NORMAL, false);
        assertEquals(permitido, escudo.cumpleRequisitos(heroe));
        if (permitido) {
            heroe.equipar(escudo);
            assertSame(escudo, heroe.getEquipo().get(ESCUDO));
        } else {
            assertThrows(IllegalArgumentException.class, () -> heroe.equipar(escudo));
            assertFalse(heroe.getEquipo().containsKey(ESCUDO));
        }
    }

    @ParameterizedTest
    @CsvSource({"arco_corto", "arco_goblin", "arco_largo", "ballesta", "vara_aprendiz", "vara_magica",
            "vara_avanzada", "vara_chaman", "garrote", "espada_pesada", "maldita"})
    void armasDeDosManosConservanLaRestriccionEnTodasLasCalidades(String codigo) {
        for (Calidad calidad : Calidad.values()) {
            assertTrue(CatalogoObjetos.equipo(codigo, calidad, false).dosManos());
        }
    }

    @Test
    void equiparGarroteDevuelveLaEspadaYElEscudoAlInventario() {
        var partida = new Partida(new Random(0) {
            @Override public int nextInt(int limite) { return limite - 1; }
        });
        partida.seleccionarHeroes(List.of(GUERRERO, ARQUERO, PICARO));
        var guerrero = partida.getEquipo().getFirst();
        var escudo = equipar(guerrero, "escudo");
        var espada = guerrero.getEquipo().get(ARMAS);
        var combate = partida.iniciarCombate(es.rpgbattlegauntlet.enums.Dificultad.PRUEBAS, 5);
        var ogro = combate.getEnemigos().getFirst();
        assertTrue(partida.getOfertas().stream().noneMatch(oferta -> oferta.objeto().codigo().equals("garrote")));
        var actor = combate.getOrdenActuacion().getFirst();
        partida.actuar(new AccionCombate(combate.getTurno(), actor.getId(), "ataque_basico", ogro.getId()));
        assertTrue(partida.getBotin().isEmpty());
        var regalos = partida.getInventario();
        partida.comprar("garrote", null);
        var garrote = partida.getInventario().getLast();

        partida.equipar(guerrero.getId(), garrote.id());

        assertSame(garrote, guerrero.getEquipo().get(ARMAS));
        assertFalse(guerrero.getEquipo().containsKey(ESCUDO));
        assertEquals(List.of(espada, escudo), partida.getInventario().stream().filter(objeto -> !regalos.contains(objeto)).toList());
        assertThrows(IllegalArgumentException.class, () -> partida.equipar(guerrero.getId(), escudo.id()));
        assertEquals(List.of(espada, escudo), partida.getInventario().stream().filter(objeto -> !regalos.contains(objeto)).toList());
        partida.equipar(guerrero.getId(), espada.id());
        partida.equipar(guerrero.getId(), escudo.id());
        assertSame(escudo, guerrero.getEquipo().get(ESCUDO));
        assertEquals(List.of(garrote), partida.getInventario().stream().filter(objeto -> !regalos.contains(objeto)).toList());
    }

    private static Heroe heroe(ClaseHeroe clase) {
        return new Heroe(clase, new EstadisticasBase(100, 20, 22, 15, 22, 5, 8, 5, 5));
    }

    private static Objeto equipar(Heroe heroe, String codigo) {
        var pieza = CatalogoObjetos.equipo(codigo, NORMAL, false);
        heroe.equipar(pieza);
        return pieza;
    }
}
