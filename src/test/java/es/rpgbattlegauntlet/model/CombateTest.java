package es.rpgbattlegauntlet.model;

import es.rpgbattlegauntlet.config.CatalogoHeroes;
import es.rpgbattlegauntlet.enums.EstadoTemporal;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.List;
import java.util.random.RandomGenerator;

import static es.rpgbattlegauntlet.enums.ClaseHeroe.*;
import static org.junit.jupiter.api.Assertions.*;

class CombateTest {

    @Test
    void ataqueFisicoDistingueEsquivaYCriticoYAplicaDefensa() {
        var guerrero = CatalogoHeroes.crearHeroe(GUERRERO);
        var rival = rival(100, 10, 40, 0, 5, 10);
        var combate = new Combate(List.of(guerrero), List.of(rival), azar(5, 10, 0));

        actuar(combate, guerrero, "ataque_basico", rival);
        actuar(combate, rival, "defender", null);
        actuar(combate, guerrero, "golpe_poderoso", rival);

        assertTrue(combate.getHistorial().getFirst().esquivado());
        assertEquals(0, combate.getHistorial().getFirst().danio());
        var golpe = combate.getHistorial().getLast();
        assertTrue(golpe.critico());
        assertEquals(10, golpe.danio());
        assertEquals(90, rival.getPvActuales());
        assertEquals(15, guerrero.getPmActuales());
    }

    @Test
    void descargaArcanaResuelveCadaEnemigoYOmiteDerrotadosEnLaCola() {
        var mago = CatalogoHeroes.crearHeroe(MAGO);
        var primero = rival(12, 1, 100, 2, 5, 0);
        var segundo = rival(100, 1, 100, 18, 5, 0);
        var combate = new Combate(List.of(mago), List.of(primero, segundo), azar(99, 99, 99, 99));

        actuar(combate, mago, "descarga_arcana", null);

        assertEquals(List.of(12, 14), combate.getHistorial().stream().map(EventoCombate::danio).toList());
        assertEquals(87, mago.getPmActuales());
        assertEquals(List.of(segundo), combate.getOrdenActuacion());
        actuar(combate, segundo, "defender", null);
        assertThrows(IllegalArgumentException.class, () -> actuar(combate, mago, "bola_fuego", primero));
        assertEquals(87, mago.getPmActuales());
    }

    @Test
    void ataqueMagicoAciertaSinTirarEsquivaNiCritico() {
        var mago = CatalogoHeroes.crearHeroe(MAGO);
        var rival = rival(120, 1, 0, 0, 5, 100);
        var combate = new Combate(List.of(mago), List.of(rival), azar());
        actuar(combate, mago, "bola_fuego", rival);
        var golpe = combate.getHistorial().getLast();
        assertEquals(48, golpe.danio());
        assertFalse(golpe.esquivado());
        assertFalse(golpe.critico());
    }

    @Test
    void curacionRespetaMaximoYRechazaEnemigos() {
        var sanador = CatalogoHeroes.crearHeroe(SANADOR);
        var rival = rival(100, 14, 0, 0, 12, 0);
        var combate = new Combate(List.of(sanador), List.of(rival), azar(99, 99));

        actuar(combate, rival, "ataque_basico", sanador);
        assertThrows(IllegalArgumentException.class, () -> actuar(combate, sanador, "curacion", rival));
        actuar(combate, sanador, "curacion", sanador);

        assertEquals(90, sanador.getPvActuales());
        assertEquals(20, combate.getHistorial().getLast().curacion());
        assertEquals(82, sanador.getPmActuales());
    }

    @Test
    void ralentizacionAlteraSiguienteCicloYPersisteHastaQueActuaElObjetivo() {
        var arquero = CatalogoHeroes.crearHeroe(ARQUERO);
        var rival = rival(100, 1, 10, 0, 16, 0);
        var lento = rival(100, 1, 10, 0, 2, 0);
        var combate = new Combate(List.of(arquero), List.of(rival, lento), azar(99, 99, 99, 99));

        actuar(combate, rival, "defender", null);
        actuar(combate, arquero, "flecha_perforante", rival);
        actuar(combate, lento, "defender", null);

        assertEquals(List.of(arquero, rival, lento), combate.getOrdenActuacion());
        assertEquals(12, rival.getVelocidadEfectiva());
        actuar(combate, arquero, "flecha_perforante", lento);
        assertEquals(1, lento.getVelocidadEfectiva());
        assertEquals(1, lento.getEstados().get(EstadoTemporal.RALENTIZACION));

        actuar(combate, rival, "defender", null);
        assertEquals(16, rival.getVelocidadEfectiva());
        actuar(combate, lento, "defender", null);
        assertEquals(List.of(rival, arquero, lento), combate.getOrdenActuacion());
    }

    @Test
    void rechazaAccionRepetidaHabilidadAjenaYFaltaDeMagia() {
        var heroe = new Heroe(MAGO, new EstadisticasBase(75, 9, 4, 5, 18, 14, 9, 5, 5));
        var rival = rival(100, 1, 0, 0, 9, 0);
        var combate = new Combate(List.of(heroe), List.of(rival), azar(99, 99));
        var fuego = new AccionCombate(1, heroe.getId(), "bola_fuego", rival.getId());

        assertThrows(IllegalArgumentException.class, () -> actuar(combate, rival, "defender", null));
        assertThrows(IllegalArgumentException.class, () -> actuar(combate, heroe, "curacion", heroe));
        combate.ejecutar(fuego);
        actuar(combate, rival, "defender", null);
        assertThrows(IllegalArgumentException.class, () -> combate.ejecutar(fuego));
        assertThrows(IllegalArgumentException.class, () -> actuar(combate, heroe, "bola_fuego", rival));
        assertEquals(List.of("ataque_basico", "defender"),
                combate.getHabilidadesDisponibles().stream().map(Habilidad::id).toList());
        assertEquals(2, combate.getHistorial().size());
        assertEquals(3, combate.getTurno());
    }

    private static Combatiente rival(int pv, int fuerza, int defensa, int resistencia, int velocidad, int esquiva) {
        return new Combatiente(new EstadisticasBase(pv, 0, fuerza, defensa, 0, resistencia, velocidad, 0, esquiva));
    }

    private static void actuar(Combate combate, Combatiente actor, String habilidad, Combatiente objetivo) {
        combate.ejecutar(new AccionCombate(combate.getTurno(), actor.getId(), habilidad,
                objetivo == null ? null : objetivo.getId()));
    }

    private static RandomGenerator azar(Integer... tiradas) {
        var pendientes = new ArrayDeque<>(List.of(tiradas));
        return new RandomGenerator() {
            @Override
            public int nextInt(int limite) {
                assertEquals(100, limite);
                return pendientes.removeFirst();
            }

            @Override
            public long nextLong() {
                throw new AssertionError("El combate usa tiradas porcentuales");
            }
        };
    }
}
