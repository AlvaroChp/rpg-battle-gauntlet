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

    private static final List<Entrada> ROBO_DRACONIANO = List.of(
            new Entrada("cimitarra_draconica", 50, false), new Entrada("ballesta", 50, false));
    private static final Map<TipoEnemigo, List<Entrada>> ROBO_DRACONICO = Map.of(
            DRACONIANO_CIMITARRA, ROBO_DRACONIANO,
            DRACONIANO_BALLESTA, ROBO_DRACONIANO,
            DRAGON_ESCARCHA, List.of(new Entrada("ventisca", 35, false),
                    new Entrada("cetro_invierno", 35, false), new Entrada("armadura_escarcha", 30, false)),
            DRAGON_OBSIDIANA, List.of(new Entrada("espada_obsidiana", 50, false),
                    new Entrada("armadura_obsidiana", 50, false)));
    private static final Map<TipoEnemigo, Tabla> TABLAS = Map.ofEntries(
            Map.entry(GOBLIN, new Tabla(35, List.of(new Entrada("daga_curva", 45),
                    new Entrada("capucha", 30), new Entrada("ligera", 25)))),
            Map.entry(GOBLIN_ARQUERO, new Tabla(40, List.of(new Entrada("arco_goblin", 75), new Entrada("daga_curva", 25, false)))),
            Map.entry(ESQUELETO, new Tabla(40, List.of(new Entrada("espada_esqueleto", 40), new Entrada("escudo_antiguo", 35), new Entrada("antigua", 25)))),
            Map.entry(CHAMAN, new Tabla(50, List.of(new Entrada("vara_chaman", 30),
                    new Entrada("tunica_chaman", 30), new Entrada("proyectil", 40)))),
            Map.entry(OGRO, new Tabla(65, List.of(new Entrada("garrote", 50), new Entrada("pesada", 30), new Entrada("botas", 20)))),
            Map.entry(CABALLERO_OSCURO, new Tabla(75, List.of(new Entrada("maldita", 40), new Entrada("oscura", 35), new Entrada("casco_oscuro", 25)))),
            Map.entry(NIGROMANTE, new Tabla(60, List.of(new Entrada("vara_osario", 35, false),
                    new Entrada("capucha_funeraria", 25), new Entrada("daga_ritual", 10, false), new Entrada("revitalizacion", 30)))),
            Map.entry(GOLEM_RUNICO, new Tabla(70, List.of(new Entrada("coraza_runica", 75),
                    new Entrada("proteccion", 25)))),
            Map.entry(VERDUGO_ESPECTRAL, new Tabla(65, List.of(new Entrada("hacha_espectral", 40, false),
                    new Entrada("yelmo_espectral", 35), new Entrada("cura_mayor", 25))))
    );

    private GeneradorBotin() {
    }

    public record BotinPosible(int probabilidad, List<Objeto> objetos, List<Objeto> armas, boolean garantizado) {}

    private static List<String> codigosGarantizados(TipoEnemigo tipo) {
        return switch (tipo) {
            case DRACONIANO_CIMITARRA -> List.of("cimitarra_draconica");
            case DRACONIANO_BALLESTA -> List.of("ballesta");
            case GUIVERNO -> List.of("aguijon_riscos");
            case DRAGON_ESCARCHA, DRAGON_OBSIDIANA ->
                    ROBO_DRACONICO.get(tipo).stream().map(Entrada::codigo).toList();
            default -> List.of();
        };
    }

    public static List<Objeto> alVencer(TipoEnemigo tipo, boolean fueRobado, RandomGenerator azar) {
        var garantizados = codigosGarantizados(tipo);
        if (!garantizados.isEmpty()) return garantizados.stream().map(codigo -> crear(codigo, azar)).toList();
        return fueRobado ? List.of() : tirar(tipo, azar).stream().toList();
    }

    public static BotinPosible consultar(TipoEnemigo tipo) {
        var garantizados = codigosGarantizados(tipo);
        if (!garantizados.isEmpty()) {
            return new BotinPosible(100, garantizados.stream().map(GeneradorBotin::objetoDeReferencia).toList(), List.of(), true);
        }
        Tabla tabla = TABLAS.get(tipo);
        if (tabla == null) return new BotinPosible(0, List.of(), List.of(), false);
        var objetos = tabla.entradas().stream().map(Entrada::codigo).map(GeneradorBotin::objetoDeReferencia).toList();
        var armas = tabla.entradas().stream().filter(Entrada::enemigoUsaArma).map(Entrada::codigo)
                .filter(codigo -> CatalogoObjetos.habilidadArma(codigo).isPresent())
                .map(GeneradorBotin::objetoDeReferencia).toList();
        return new BotinPosible(tabla.probabilidad(), objetos, armas, false);
    }

    private static Objeto objetoDeReferencia(String codigo) {
        if (CatalogoObjetos.codigosPergaminos().contains(codigo)) return CatalogoObjetos.pergamino(codigo);
        if (codigo.equals("revitalizacion")) return CatalogoObjetos.pocionRevitalizacion();
        if (codigo.equals("ballesta")) return CatalogoObjetos.equipo(codigo, Calidad.SUPERIOR, false);
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
        return Optional.of(elegirObjeto(tabla.entradas(), azar));
    }

    public static boolean permiteRobo(TipoEnemigo tipo) {
        return tipo == DRAGON || TABLAS.containsKey(tipo) || ROBO_DRACONICO.containsKey(tipo);
    }

    public static Objeto elegirBotinRobado(TipoEnemigo tipo, RandomGenerator azar) {
        if (tipo == DRAGON) return CatalogoObjetos.requiemDelDragon();
        var entradas = ROBO_DRACONICO.get(tipo);
        return elegirObjeto(entradas != null ? entradas : TABLAS.get(tipo).entradas(), azar);
    }

    private static Objeto elegirObjeto(List<Entrada> entradas, RandomGenerator azar) {
        int tirada = azar.nextInt(100);
        for (Entrada entrada : entradas) {
            if (tirada < entrada.peso()) {
                return crear(entrada.codigo(), azar);
            }
            tirada -= entrada.peso();
        }
        throw new IllegalStateException("Los pesos de botín deben sumar 100");
    }

    private static Objeto crear(String codigo, RandomGenerator azar) {
        return switch (codigo) {
            case "proyectil", "proteccion", "cura_mayor", "ventisca" -> CatalogoObjetos.pergamino(codigo);
            case "ballesta" -> CatalogoObjetos.equipo(codigo, Calidad.SUPERIOR, false);
            case "revitalizacion" -> CatalogoObjetos.pocionRevitalizacion();
            default -> CatalogoObjetos.equipo(codigo, Calidad.values()[tramo(azar.nextInt(100))], false);
        };
    }

    private static int tramo(int tirada) {
        return tirada < 35 ? 0 : tirada < 85 ? 1 : 2;
    }
}
