package es.rpgbattlegauntlet.model;

import es.rpgbattlegauntlet.config.CatalogoHeroes;
import es.rpgbattlegauntlet.enums.ClaseHeroe;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class HeroeTest {

    @ParameterizedTest
    @CsvSource({
            "GUERRERO, 150, 20, 15, 15, 3, 8, 6, 5, 5",
            "MAGO, 75, 100, 4, 5, 18, 14, 9, 5, 5",
            "ARQUERO, 100, 30, 13, 7, 5, 8, 14, 15, 10",
            "SANADOR, 90, 90, 5, 8, 15, 13, 8, 5, 5",
            "PICARO, 85, 25, 12, 6, 4, 7, 17, 20, 15"
    })
    void heroeEmpiezaConLosAtributosDeSuClase(
            ClaseHeroe clase, int pv, int pm, int fuerza, int defensa,
            int inteligencia, int resistencia, int velocidad, int critico, int esquiva
    ) {
        var heroe = CatalogoHeroes.crearHeroe(clase);

        assertEquals(clase, heroe.getClase());
        assertEquals(new EstadisticasBase(pv, pm, fuerza, defensa, inteligencia,
                resistencia, velocidad, critico, esquiva), heroe.getEstadisticasBase());
        assertEquals(pv, heroe.getPvActuales());
        assertEquals(pm, heroe.getPmActuales());
        assertNotEquals(heroe.getId(), CatalogoHeroes.crearHeroe(clase).getId());
    }
}
