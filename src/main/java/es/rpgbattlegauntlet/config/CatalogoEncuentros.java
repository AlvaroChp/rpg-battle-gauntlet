package es.rpgbattlegauntlet.config;

import es.rpgbattlegauntlet.enums.Dificultad;
import es.rpgbattlegauntlet.enums.TipoEnemigo;
import es.rpgbattlegauntlet.model.Enemigo;

import java.util.List;
import java.util.stream.Stream;

import static es.rpgbattlegauntlet.enums.TipoEnemigo.*;

public final class CatalogoEncuentros {

    private static final List<List<TipoEnemigo>> RONDAS_COMUNES = List.of(
            List.of(GOBLIN),
            List.of(GOBLIN, GOBLIN_ARQUERO),
            List.of(GOBLIN, ESQUELETO),
            List.of(ESQUELETO, CHAMAN, GOBLIN_ARQUERO),
            List.of(OGRO),
            List.of(ESQUELETO, ESQUELETO, CHAMAN),
            List.of(CABALLERO_OSCURO),
            List.of(CABALLERO_OSCURO, CHAMAN),
            List.of(CABALLERO_OSCURO, CABALLERO_OSCURO)
    );

    private static final List<TipoEnemigo> JEFE_FINAL = List.of(DRAGON);
    private static final List<List<TipoEnemigo>> RONDAS_HABITUALES =
            Stream.concat(RONDAS_COMUNES.stream(), Stream.of(JEFE_FINAL)).toList();
    private static final List<List<TipoEnemigo>> RONDAS_AMPLIADAS = Stream.concat(RONDAS_COMUNES.stream(), Stream.of(
            List.of(VERDUGO_ESPECTRAL),
            List.of(NIGROMANTE, CABALLERO_OSCURO),
            List.of(GOLEM_RUNICO),
            List.of(NIGROMANTE, NIGROMANTE),
            List.of(CABALLERO_OSCURO, CABALLERO_OSCURO, NIGROMANTE),
            JEFE_FINAL
    )).toList();

    private CatalogoEncuentros() {
    }

    public static int totalRondas() {
        return totalRondas(Dificultad.FACIL);
    }

    public static int totalRondas(Dificultad dificultad) {
        return rondas(dificultad).size();
    }

    private static List<List<TipoEnemigo>> rondas(Dificultad dificultad) {
        return switch (dificultad) {
            case EXPERTO, PRUEBAS -> RONDAS_AMPLIADAS;
            case FACIL, DIFICIL -> RONDAS_HABITUALES;
        };
    }

    public static List<Enemigo> crearEnemigos(int ronda) {
        return crearEnemigos(ronda, Dificultad.FACIL);
    }

    public static List<Enemigo> crearEnemigos(int ronda, Dificultad dificultad) {
        var rondas = rondas(dificultad);
        if (ronda < 1 || ronda > rondas.size()) {
            throw new IllegalArgumentException("La ronda está fuera del catálogo");
        }
        return rondas.get(ronda - 1).stream().map(tipo -> CatalogoEnemigos.crear(tipo, dificultad)).toList();
    }
}
