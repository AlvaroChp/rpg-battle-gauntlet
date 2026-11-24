package es.rpgbattlegauntlet.config;

import es.rpgbattlegauntlet.enums.ClaseHeroe;
import es.rpgbattlegauntlet.model.EstadisticasBase;
import es.rpgbattlegauntlet.model.Heroe;

import java.util.Map;
import java.util.Objects;

public final class CatalogoHeroes {

    private static final Map<ClaseHeroe, EstadisticasBase> ESTADISTICAS_INICIALES = Map.of(
            ClaseHeroe.GUERRERO, new EstadisticasBase(150, 20, 15, 15, 3, 8, 6, 5, 5, 10),
            ClaseHeroe.MAGO, new EstadisticasBase(75, 100, 4, 5, 18, 14, 9, 5, 5, 3),
            ClaseHeroe.ARQUERO, new EstadisticasBase(100, 30, 13, 7, 5, 8, 14, 15, 10, 15),
            ClaseHeroe.SANADOR, new EstadisticasBase(90, 90, 5, 8, 15, 13, 8, 5, 5, 3),
            ClaseHeroe.PICARO, new EstadisticasBase(85, 25, 12, 6, 4, 7, 17, 20, 15, 25)
    );

    private CatalogoHeroes() {
    }

    public static Heroe crearHeroe(ClaseHeroe clase) {
        Objects.requireNonNull(clase, "La clase del héroe es obligatoria");
        return new Heroe(clase, ESTADISTICAS_INICIALES.get(clase));
    }
}
