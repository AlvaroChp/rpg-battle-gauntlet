package es.rpgbattlegauntlet.config;

import es.rpgbattlegauntlet.enums.Dificultad;
import es.rpgbattlegauntlet.enums.TipoEnemigo;
import es.rpgbattlegauntlet.model.Enemigo;

import java.util.List;

import static es.rpgbattlegauntlet.enums.TipoEnemigo.*;

public final class CatalogoEncuentros {

    private static final List<List<TipoEnemigo>> RONDAS = List.of(
            List.of(GOBLIN),
            List.of(GOBLIN, GOBLIN),
            List.of(GOBLIN, ESQUELETO),
            List.of(ESQUELETO, CHAMAN),
            List.of(OGRO),
            List.of(GOBLIN, GOBLIN, CHAMAN),
            List.of(CABALLERO_OSCURO),
            List.of(CABALLERO_OSCURO, CHAMAN),
            List.of(CABALLERO_OSCURO, CABALLERO_OSCURO),
            List.of(DRAGON)
    );

    private CatalogoEncuentros() {
    }

    public static int totalRondas() {
        return RONDAS.size();
    }

    public static List<Enemigo> crearEnemigos(int ronda) {
        return crearEnemigos(ronda, Dificultad.FACIL);
    }

    public static List<Enemigo> crearEnemigos(int ronda, Dificultad dificultad) {
        if (ronda < 1 || ronda > totalRondas()) {
            throw new IllegalArgumentException("La ronda está fuera del catálogo");
        }
        return RONDAS.get(ronda - 1).stream().map(tipo -> CatalogoEnemigos.crear(tipo, dificultad)).toList();
    }
}
