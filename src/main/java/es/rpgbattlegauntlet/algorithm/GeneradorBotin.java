package es.rpgbattlegauntlet.algorithm;

import es.rpgbattlegauntlet.config.CatalogoObjetos;
import es.rpgbattlegauntlet.enums.Calidad;
import es.rpgbattlegauntlet.enums.PotenciaPergamino;
import es.rpgbattlegauntlet.enums.TipoEnemigo;
import es.rpgbattlegauntlet.model.Habilidad;
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
            GOBLIN, new Tabla(35, List.of(new Entrada("daga", 25), new Entrada("daga_duelo", 12), new Entrada("daga_curva", 8),
                    new Entrada("capucha", 30), new Entrada("ligera", 25))),
            GOBLIN_ARQUERO, new Tabla(40, List.of(new Entrada("arco_goblin", 100))),
            ESQUELETO, new Tabla(40, List.of(new Entrada("espada_esqueleto", 40), new Entrada("escudo", 35), new Entrada("antigua", 25))),
            CHAMAN, new Tabla(50, List.of(new Entrada("vara_magica", 30), new Entrada("vara_chaman", 15),
                    new Entrada("tunica_mago", 20), new Entrada("tunica_chaman", 15), new Entrada("pergamino", 20))),
            OGRO, new Tabla(65, List.of(new Entrada("garrote", 50), new Entrada("pesada", 30), new Entrada("botas", 20))),
            CABALLERO_OSCURO, new Tabla(75, List.of(new Entrada("maldita", 40), new Entrada("oscura", 35), new Entrada("casco_oscuro", 25)))
    );

    private GeneradorBotin() {
    }

    public static List<Habilidad> habilidadesDeArmas(TipoEnemigo tipo) {
        Tabla tabla = TABLAS.get(tipo);
        if (tabla == null) return List.of();
        return tabla.entradas().stream().map(Entrada::codigo)
                .flatMap(codigo -> CatalogoObjetos.habilidadArma(codigo).stream()).toList();
    }

    public static Optional<Objeto> tirar(TipoEnemigo enemigo, RandomGenerator azar) {
        Tabla tabla = TABLAS.get(enemigo);
        if (tabla == null || azar.nextInt(100) >= tabla.probabilidad()) {
            return Optional.empty();
        }
        return Optional.of(elegirObjeto(tabla, azar));
    }

    public static boolean permiteRobo(TipoEnemigo tipo) {
        return tipo == DRAGON || TABLAS.containsKey(tipo);
    }

    public static Objeto elegirBotinRobado(TipoEnemigo tipo, RandomGenerator azar) {
        return tipo == DRAGON ? CatalogoObjetos.requiemDelDragon() : elegirObjeto(TABLAS.get(tipo), azar);
    }

    private static Objeto elegirObjeto(Tabla tabla, RandomGenerator azar) {
        int tirada = azar.nextInt(100);
        for (Entrada entrada : tabla.entradas()) {
            if (tirada < entrada.peso()) {
                return crear(entrada.codigo(), azar);
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
