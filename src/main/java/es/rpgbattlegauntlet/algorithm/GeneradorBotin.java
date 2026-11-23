package es.rpgbattlegauntlet.algorithm;

import es.rpgbattlegauntlet.config.CatalogoObjetos;
import es.rpgbattlegauntlet.enums.Calidad;
import es.rpgbattlegauntlet.enums.PotenciaPergamino;
import es.rpgbattlegauntlet.enums.TipoEnemigo;
import es.rpgbattlegauntlet.model.Objeto;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.random.RandomGenerator;

import static es.rpgbattlegauntlet.enums.TipoEnemigo.*;

public final class GeneradorBotin {
    private record Entrada(String codigo, int peso) {}
    private record Tabla(int probabilidad, List<Entrada> entradas) {}

    private static final Map<TipoEnemigo, Tabla> TABLAS = Map.of(
            GOBLIN, new Tabla(35, List.of(new Entrada("daga", 45), new Entrada("capucha", 30), new Entrada("ligera", 25))),
            ESQUELETO, new Tabla(40, List.of(new Entrada("espada_esqueleto", 40), new Entrada("escudo", 35), new Entrada("antigua", 25))),
            CHAMAN, new Tabla(50, List.of(new Entrada("vara_magica", 45), new Entrada("tunica_mago", 35), new Entrada("pergamino", 20))),
            OGRO, new Tabla(65, List.of(new Entrada("garrote", 50), new Entrada("pesada", 30), new Entrada("botas", 20))),
            CABALLERO_OSCURO, new Tabla(75, List.of(new Entrada("maldita", 40), new Entrada("oscura", 35), new Entrada("casco_oscuro", 25)))
    );

    private GeneradorBotin() {
    }

    public static Optional<Objeto> tirar(TipoEnemigo enemigo, RandomGenerator azar) {
        Tabla tabla = TABLAS.get(enemigo);
        if (tabla == null || azar.nextInt(100) >= tabla.probabilidad()) {
            return Optional.empty();
        }

        int tirada = azar.nextInt(100);
        for (Entrada entrada : tabla.entradas()) {
            if (tirada < entrada.peso()) {
                return Optional.of(crear(entrada.codigo(), azar));
            }
            tirada -= entrada.peso();
        }
        throw new IllegalStateException("Los pesos de botín deben sumar 100");
    }

    private static Objeto crear(String codigo, RandomGenerator azar) {
        if ("pergamino".equals(codigo)) {
            var hechizos = CatalogoObjetos.codigosPergaminos();
            String hechizo = hechizos.get(azar.nextInt(hechizos.size()));
            int potencia = tramo(azar.nextInt(100));
            return CatalogoObjetos.pergamino(hechizo, PotenciaPergamino.values()[potencia]);
        }
        return CatalogoObjetos.equipo(codigo, Calidad.values()[tramo(azar.nextInt(100))], false);
    }

    private static int tramo(int tirada) {
        return tirada < 35 ? 0 : tirada < 85 ? 1 : 2;
    }
}
