package es.rpgbattlegauntlet.algorithm;

import es.rpgbattlegauntlet.config.CatalogoObjetos;
import es.rpgbattlegauntlet.enums.Calidad;
import es.rpgbattlegauntlet.enums.TipoEnemigo;
import es.rpgbattlegauntlet.model.Habilidad;
import es.rpgbattlegauntlet.model.Objeto;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.random.RandomGenerator;

import static es.rpgbattlegauntlet.enums.TipoEnemigo.*;

public final class GeneradorBotin {
    private record Entrada(String codigo, int peso, boolean enemigoUsaArma) {
        Entrada(String codigo, int peso) {
            this(codigo, peso, true);
        }
    }
    private record Tabla(int probabilidad, List<Entrada> entradas) {}

    private static final Map<TipoEnemigo, Tabla> TABLAS = Map.of(
            GOBLIN, new Tabla(35, List.of(new Entrada("daga_curva", 45),
                    new Entrada("capucha", 30), new Entrada("ligera", 25))),
            GOBLIN_ARQUERO, new Tabla(40, List.of(new Entrada("arco_goblin", 75), new Entrada("daga_curva", 25, false))),
            ESQUELETO, new Tabla(40, List.of(new Entrada("espada_esqueleto", 40), new Entrada("escudo", 35), new Entrada("antigua", 25))),
            CHAMAN, new Tabla(50, List.of(new Entrada("vara_chaman", 30),
                    new Entrada("tunica_chaman", 30), new Entrada("proyectil", 40))),
            OGRO, new Tabla(65, List.of(new Entrada("garrote", 50), new Entrada("pesada", 30), new Entrada("botas", 20))),
            CABALLERO_OSCURO, new Tabla(75, List.of(new Entrada("maldita", 40), new Entrada("oscura", 35), new Entrada("casco_oscuro", 25))),
            NIGROMANTE, new Tabla(60, List.of(new Entrada("vara_osario", 35, false),
                    new Entrada("capucha_funeraria", 25), new Entrada("daga_ritual", 10, false), new Entrada("revitalizacion", 30))),
            GOLEM_RUNICO, new Tabla(70, List.of(new Entrada("guantes_runicos", 40),
                    new Entrada("coraza_runica", 35), new Entrada("proteccion", 25))),
            VERDUGO_ESPECTRAL, new Tabla(65, List.of(new Entrada("hacha_espectral", 40, false),
                    new Entrada("yelmo_espectral", 35), new Entrada("cura_mayor", 25)))
    );

    private GeneradorBotin() {
    }

    public record BotinPosible(int probabilidad, List<Objeto> objetos, List<Objeto> armas) {}

    public static BotinPosible consultar(TipoEnemigo tipo) {
        Tabla tabla = TABLAS.get(tipo);
        if (tabla == null) return new BotinPosible(0, List.of(), List.of());
        var objetos = tabla.entradas().stream().map(Entrada::codigo).map(GeneradorBotin::objetoDeReferencia).toList();
        var armas = tabla.entradas().stream().filter(Entrada::enemigoUsaArma).map(Entrada::codigo)
                .filter(codigo -> CatalogoObjetos.habilidadArma(codigo).isPresent())
                .map(GeneradorBotin::objetoDeReferencia).toList();
        return new BotinPosible(tabla.probabilidad(), objetos, armas);
    }

    private static Objeto objetoDeReferencia(String codigo) {
        if (CatalogoObjetos.codigosPergaminos().contains(codigo)) return CatalogoObjetos.pergamino(codigo);
        if (codigo.equals("revitalizacion")) return CatalogoObjetos.pocionRevitalizacion();
        return CatalogoObjetos.equipo(codigo, Calidad.NORMAL, false);
    }

    public static List<Habilidad> habilidadesDeArmas(TipoEnemigo tipo) {
        Tabla tabla = TABLAS.get(tipo);
        if (tabla == null) return List.of();
        return tabla.entradas().stream().filter(Entrada::enemigoUsaArma).map(Entrada::codigo)
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
        return switch (codigo) {
            case "proyectil", "proteccion", "cura_mayor" -> CatalogoObjetos.pergamino(codigo);
            case "revitalizacion" -> CatalogoObjetos.pocionRevitalizacion();
            default -> CatalogoObjetos.equipo(codigo, Calidad.values()[tramo(azar.nextInt(100))], false);
        };
    }

    private static int tramo(int tirada) {
        return tirada < 35 ? 0 : tirada < 85 ? 1 : 2;
    }
}
