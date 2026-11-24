package es.rpgbattlegauntlet.model;

import es.rpgbattlegauntlet.config.CatalogoEncuentros;
import es.rpgbattlegauntlet.config.Equilibrio;
import es.rpgbattlegauntlet.enums.Dificultad;
import es.rpgbattlegauntlet.enums.EstadoPartida;
import es.rpgbattlegauntlet.enums.TipoEnemigo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static es.rpgbattlegauntlet.enums.ClaseHeroe.*;
import static es.rpgbattlegauntlet.enums.TipoEnemigo.*;
import static org.junit.jupiter.api.Assertions.*;

class EncuentrosTest {

    private static final List<List<TipoEnemigo>> ENCUENTROS_ESPERADOS = List.of(
            List.of(GOBLIN),
            List.of(GOBLIN, GOBLIN_ARQUERO),
            List.of(GOBLIN, ESQUELETO),
            List.of(ESQUELETO, CHAMAN, GOBLIN_ARQUERO),
            List.of(OGRO),
            List.of(ESQUELETO, ESQUELETO, CHAMAN),
            List.of(CABALLERO_OSCURO),
            List.of(CABALLERO_OSCURO, CHAMAN),
            List.of(CABALLERO_OSCURO, CABALLERO_OSCURO),
            List.of(DRAGON)
    );

    @Test
    void catalogoRechazaRondasFueraDelIntervalo() {
        assertEquals(10, CatalogoEncuentros.totalRondas());
        assertThrows(IllegalArgumentException.class, () -> CatalogoEncuentros.crearEnemigos(0));
        assertThrows(IllegalArgumentException.class, () -> CatalogoEncuentros.crearEnemigos(11));
    }

    @Test
    void expertoYPruebasAmplianLaCampanaYElDragonSiempreEsElUltimo() {
        for (Dificultad dificultad : Dificultad.values()) {
            int esperado = dificultad == Dificultad.EXPERTO || dificultad == Dificultad.PRUEBAS ? 15 : 10;
            assertEquals(esperado, CatalogoEncuentros.totalRondas(dificultad));
            assertEquals(List.of(DRAGON), CatalogoEncuentros.crearEnemigos(esperado, dificultad).stream()
                    .map(Enemigo::getTipo).toList());
            assertThrows(IllegalArgumentException.class,
                    () -> CatalogoEncuentros.crearEnemigos(esperado + 1, dificultad));
        }
    }

    @ParameterizedTest
    @EnumSource(value = Dificultad.class, names = {"EXPERTO", "PRUEBAS"})
    void campanaAmpliadaTerminaConElDragonEnLaRonda15(Dificultad dificultad) {
        var partida = partidaSinCriticosNiEsquivas();
        partida.seleccionarHeroes(List.of(PICARO, ARQUERO, GUERRERO));
        partida.iniciarCombate(dificultad);
        var tramoAmpliado = List.of(
                List.of(VERDUGO_ESPECTRAL),
                List.of(NIGROMANTE, CABALLERO_OSCURO),
                List.of(GOLEM_RUNICO),
                List.of(NIGROMANTE, NIGROMANTE),
                List.of(CABALLERO_OSCURO, CABALLERO_OSCURO, NIGROMANTE),
                List.of(DRAGON));
        assertEquals(15, partida.getTotalRondas());

        for (int ronda = 1; ronda <= 15; ronda++) {
            var esperados = ronda < 10 ? ENCUENTROS_ESPERADOS.get(ronda - 1) : tramoAmpliado.get(ronda - 10);
            var enemigos = partida.getCombate().getEnemigos();
            assertEquals(esperados, enemigos.stream().map(Enemigo.class::cast).map(Enemigo::getTipo).toList());
            int oroAntes = partida.getOro();

            ganarEncuentroDePrueba(partida);

            if (ronda < 15) {
                assertEquals(EstadoPartida.ENTRE_COMBATES, partida.getEstado());
                assertEquals(ronda, partida.getRecompensa().ronda());
                if (dificultad == Dificultad.PRUEBAS) {
                    assertTrue(partida.tieneOroIlimitado());
                    assertEquals(oroAntes, partida.getOro());
                } else {
                    assertTrue(partida.getOro() > oroAntes);
                }
                if (ronda == 14) {
                    assertEquals(371, partida.getRecompensa().experiencia());
                    for (var heroe : partida.getEquipo()) {
                        assertEquals(12, heroe.getNivel());
                        assertEquals(0, heroe.getExperiencia());
                        assertEquals(35, heroe.getPuntos());
                    }
                }
                partida.getEquipo().forEach(heroe -> heroe.recibirCuracion(heroe.getEstadisticasBase().pvMaximos()));
                partida.avanzarRonda(ronda);
            } else {
                assertEquals(EstadoPartida.VICTORIA, partida.getEstado());
                assertNull(partida.getRecompensa());
                assertEquals(oroAntes, partida.getOro());
            }
        }
        assertThrows(IllegalStateException.class, () -> partida.avanzarRonda(15));
    }

    @Test
    void dificilAumentaLaVidaYMultiplicaElDanioPorUnoYMedio() {
        var goblin = CatalogoEncuentros.crearEnemigos(1, Dificultad.DIFICIL).getFirst();
        assertEquals(81, goblin.getPvActuales());
        assertEquals(1.5, goblin.getMultiplicadorDanio());
    }

    @Test
    void modoPruebasCreaTodosLosEnemigosConUnPuntoDeVida() {
        for (int ronda = 1; ronda <= CatalogoEncuentros.totalRondas(Dificultad.PRUEBAS); ronda++) {
            for (var enemigo : CatalogoEncuentros.crearEnemigos(ronda, Dificultad.PRUEBAS)) {
                assertEquals(1, enemigo.getPvActuales());
                assertEquals(1, enemigo.getEstadisticasBase().pvMaximos());
                assertEquals(1.0, enemigo.getMultiplicadorDanio());
            }
        }
    }

    @Test
    void recorreLasDiezRondasEnOrdenYTerminaEnVictoria() {
        var partida = partidaSinCriticosNiEsquivas();
        assertEquals(0, partida.getRonda());
        assertNull(partida.getCombate());
        assertThrows(IllegalStateException.class, partida::iniciarCombate);
        partida.seleccionarHeroes(List.of(PICARO, ARQUERO, GUERRERO));
        var combate = partida.iniciarCombate();
        assertThrows(IllegalStateException.class, partida::iniciarCombate);
        assertThrows(IllegalStateException.class, () -> partida.seleccionarHeroes(List.of(MAGO, PICARO, GUERRERO)));
        var idsEnemigos = new HashSet<UUID>();
        int ultimoTurno = 0;

        for (int ronda = 1; ronda <= ENCUENTROS_ESPERADOS.size(); ronda++) {
            int numero = ronda;
            assertEquals(numero, partida.getRonda());
            assertSame(combate, partida.getCombate());
            assertEquals(EstadoPartida.COMBATE, partida.getEstado());
            assertTrue(combate.getTurno() > ultimoTurno);
            assertThrows(IllegalStateException.class, () -> partida.avanzarRonda(numero));
            assertEquals(ENCUENTROS_ESPERADOS.get(numero - 1), combate.getEnemigos().stream()
                    .map(Enemigo.class::cast).map(Enemigo::getTipo).toList());

            for (var enemigo : combate.getEnemigos()) {
                assertTrue(idsEnemigos.add(enemigo.getId()));
                assertEquals(enemigo.getEstadisticasBase().pvMaximos(), enemigo.getPvActuales());
            }
            ganarEncuentroDePrueba(partida);
            ultimoTurno = combate.getTurno();
            if (numero < ENCUENTROS_ESPERADOS.size()) {
                assertEquals(EstadoPartida.ENTRE_COMBATES, partida.getEstado());
                assertThrows(IllegalStateException.class, () -> partida.avanzarRonda(numero + 1));
                combate = partida.avanzarRonda(numero);
            }
        }

        assertEquals(EstadoPartida.VICTORIA, partida.getEstado());
        assertEquals(10, partida.getRonda());
        assertNull(partida.getRecompensa());
        assertTrue(partida.getBotin().isEmpty());
        assertThrows(IllegalStateException.class, () -> partida.avanzarRonda(10));
        assertThrows(IllegalStateException.class, partida::iniciarCombate);
    }

    @Test
    void conservaPvPmEIdentidadYRechazaAccionesDeLaRondaAnterior() {
        var partida = partidaSinCriticosNiEsquivas();
        partida.seleccionarHeroes(List.of(PICARO, ARQUERO, GUERRERO));
        var primero = partida.iniciarCombate();
        var equipo = partida.getEquipo();
        var picaro = equipo.getFirst();
        var arquero = equipo.get(1);
        var guerrero = equipo.getLast();
        picaro.recibirDanio(15);
        partida.actuar(new AccionCombate(primero.getTurno(), picaro.getId(), "esquivar", null));
        partida.actuar(new AccionCombate(primero.getTurno(), arquero.getId(), "defender", null));
        arquero.recibirDanio(arquero.getPvActuales());
        int pvTrasElCombate = picaro.getPvActuales();
        var enemigo = primero.getEnemigos().getFirst();
        enemigo.recibirDanio(enemigo.getPvActuales() - 1);
        var accionAnterior = new AccionCombate(primero.getTurno(), guerrero.getId(), "ataque_basico", enemigo.getId());
        partida.actuar(accionAnterior);

        assertEquals(2, partida.getRecompensa().supervivientes());
        assertEquals(2, picaro.getNivel());
        assertEquals(2, guerrero.getNivel());
        assertEquals(1, arquero.getNivel());
        assertEquals(0, arquero.getExperiencia());
        assertEquals(0, arquero.getPuntos());

        var segundo = partida.avanzarRonda(1);

        assertEquals(equipo, partida.getEquipo());
        assertEquals(Math.min(85, pvTrasElCombate + Equilibrio.PV_AL_SUBIR), picaro.getPvActuales());
        assertEquals(25, picaro.getPmActuales());
        assertFalse(arquero.estaVivo());
        assertFalse(segundo.getOrdenActuacion().contains(arquero));
        assertTrue(picaro.getEstados().isEmpty());
        assertThrows(IllegalStateException.class, () -> partida.avanzarRonda(1));
        assertThrows(IllegalArgumentException.class, () -> segundo.ejecutar(
                new AccionCombate(accionAnterior.turno(), picaro.getId(), "defender", null)));
        ganarEncuentroDePrueba(partida);
        assertThrows(IllegalStateException.class, () -> partida.avanzarRonda(1));
    }

    @Test
    void derrotaImpideAvanzar() {
        var partida = partidaSinCriticosNiEsquivas();
        partida.seleccionarHeroes(List.of(GUERRERO, MAGO, SANADOR));
        partida.getEquipo().forEach(heroe -> heroe.recibirDanio(heroe.getPvActuales() - 1));
        var combate = partida.iniciarCombate();

        while (partida.getEstado() == EstadoPartida.COMBATE) {
            var actor = combate.getOrdenActuacion().getFirst();
            combate.ejecutar(new AccionCombate(combate.getTurno(), actor.getId(), "defender", null));
        }

        assertEquals(EstadoPartida.DERROTA, partida.getEstado());
        assertEquals(1, partida.getRonda());
        assertThrows(IllegalStateException.class, () -> partida.avanzarRonda(1));
        assertThrows(IllegalStateException.class, partida::iniciarCombate);
    }

    private static Partida partidaSinCriticosNiEsquivas() {
        return new Partida(new Random(0) {
            @Override
            public int nextInt(int limite) {
                return limite - 1;
            }
        });
    }

    private static void ganarEncuentroDePrueba(Partida partida) {
        var combate = partida.getCombate();
        // Se aísla el avance de la campaña con enemigos a 1 PV y se evita que el nigromante los reanime.
        var ordenObjetivos = new ArrayList<>(combate.getEnemigos());
        ordenObjetivos.sort(Comparator.comparingInt(enemigo -> ((Enemigo) enemigo).getTipo() == NIGROMANTE ? 0 : 1));
        int acciones = 0;
        while (partida.getEstado() == EstadoPartida.COMBATE) {
            assertTrue(++acciones <= ordenObjetivos.size() * 3, "El combate de prueba debe terminar aunque haya reanimaciones");
            var enemigo = ordenObjetivos.stream().filter(Combatiente::estaVivo).findFirst().orElseThrow();
            var actor = combate.getOrdenActuacion().getFirst();
            enemigo.recibirDanio(enemigo.getPvActuales() - 1);
            partida.actuar(new AccionCombate(combate.getTurno(), actor.getId(), "ataque_basico", enemigo.getId()));
        }
    }
}
