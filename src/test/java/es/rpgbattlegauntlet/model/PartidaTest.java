package es.rpgbattlegauntlet.model;

import es.rpgbattlegauntlet.enums.ClaseHeroe;
import es.rpgbattlegauntlet.enums.EstadoPartida;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static es.rpgbattlegauntlet.enums.ClaseHeroe.*;
import static org.junit.jupiter.api.Assertions.*;

class PartidaTest {

    @Test
    void partidaNuevaTieneIdentidadPropiaYEsperaSeleccion() {
        var partida = new Partida();

        assertNotEquals(new Partida().getId(), partida.getId());
        assertEquals(EstadoPartida.SELECCION_PERSONAJES, partida.getEstado());
        assertTrue(partida.getEquipo().isEmpty());
    }

    @Test
    void seleccionValidaConservaOrdenEIdentidadAlRepetirse() {
        var partida = new Partida();
        var clases = List.of(ARQUERO, GUERRERO, PICARO);

        partida.seleccionarHeroes(clases);
        var equipo = partida.getEquipo();
        partida.seleccionarHeroes(clases);

        assertEquals(clases, equipo.stream().map(Heroe::getClase).toList());
        assertEquals(equipo, partida.getEquipo());
        assertEquals(3, equipo.stream().map(Heroe::getId).distinct().count());
        assertThrows(UnsupportedOperationException.class, equipo::clear);
    }

    @Test
    void seleccionInvalidaConservaElEquipoAnterior() {
        var partida = new Partida();
        partida.seleccionarHeroes(List.of(GUERRERO, MAGO, SANADOR));
        var equipo = partida.getEquipo();

        List<List<ClaseHeroe>> invalidas = Arrays.asList(
                null,
                List.of(),
                List.of(GUERRERO, MAGO),
                List.of(GUERRERO, MAGO, SANADOR, ARQUERO),
                List.of(GUERRERO, GUERRERO, MAGO),
                Arrays.asList(GUERRERO, null, MAGO)
        );

        for (var clases : invalidas) {
            assertThrows(IllegalArgumentException.class, () -> partida.seleccionarHeroes(clases));
            assertEquals(equipo, partida.getEquipo());
        }
    }
}
