package es.rpgbattlegauntlet.config;

import es.rpgbattlegauntlet.enums.*;
import es.rpgbattlegauntlet.model.*;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static es.rpgbattlegauntlet.enums.Categoria.*;
import static es.rpgbattlegauntlet.enums.TipoObjetivo.*;

public final class CatalogoObjetos {
    private record Equipo(String codigo, String inferior, String normal, String superior, Categoria categoria,
                          Bonificaciones bonos, int fuerza, int inteligencia, int precio, Habilidad habilidad,
                          boolean enTienda) {
        Objeto plantilla(Calidad calidad) {
            String nombre = switch (calidad) {
                case INFERIOR -> inferior;
                case NORMAL -> normal;
                case SUPERIOR -> superior;
            };
            return new Objeto(null, codigo, nombre, categoria, calidad, null, false, bonos.ajustar(calidad), fuerza,
                    inteligencia, precio, calidad.venta(precio), calidad.recompra(precio), habilidad, null);
        }
    }

    private record Pergamino(String codigo, String nombre, TipoConsumible tipo, TipoObjetivo objetivo,
                             int debil, int normal, int poderoso, int inteligencia, int precio) {
        Objeto plantilla(PotenciaPergamino potencia) {
            int cantidad = switch (potencia) {
                case DEBIL -> debil;
                case NORMAL -> normal;
                case PODEROSO -> poderoso;
            };
            int proteccion = tipo == TipoConsumible.PROTECCION ? cantidad
                    : tipo == TipoConsumible.CURA_TOTAL && potencia == PotenciaPergamino.PODEROSO ? 35 : 0;
            int venta = potencia.venta(precio);
            return new Objeto(null, codigo, nombre, PERGAMINOS, null, potencia, false, Bonificaciones.NINGUNA,
                    0, potencia.requisito(inteligencia), precio, venta, 2 * venta, null,
                    new EfectoConsumible(tipo, objetivo, cantidad, 0, proteccion));
        }
    }

    private static final Habilidad DAGA = habilidad("estocada_rapida", "Estocada rápida", 3, false, 5, 20, 5, 0);
    private static final Habilidad ESPADA = habilidad("corte", "Corte amplio", 4, false, 10, 10, 0, 0);
    private static final Habilidad ARCO = habilidad("disparo_tenso", "Disparo tenso", 5, false, 10, 15, 0, 25);
    private static final Habilidad VARA = habilidad("pulso", "Pulso de vara", 5, true, 13, 10, 0, 0);
    private static final Habilidad GARROTE = habilidad("impacto", "Impacto contundente", 5, false, 14, 0, 0, 15);

    private static final Map<String, Equipo> EQUIPOS = equipos();
    private static final List<Pergamino> PERGAMINOS_DISPONIBLES = List.of(
            perg("proyectil", "Proyectil mágico", TipoConsumible.DANIO, ENEMIGO, 50, 65, 90, 5, 65),
            perg("cura", "Cura", TipoConsumible.CURACION, ALIADO, 65, 90, 130, 6, 75),
            perg("cura_mayor", "Cura mayor", TipoConsumible.CURACION, ALIADO, 90, 120, 160, 8, 100),
            perg("llamarada", "Llamarada", TipoConsumible.DANIO, ENEMIGO, 75, 100, 140, 10, 110),
            perg("ventisca", "Ventisca", TipoConsumible.DANIO, ENEMIGOS, 45, 65, 90, 12, 140),
            perg("cura_grupal", "Cura grupal", TipoConsumible.CURACION, ALIADOS, 50, 70, 100, 14, 150),
            perg("tormenta", "Tormenta eléctrica", TipoConsumible.DANIO, ENEMIGOS, 60, 80, 115, 15, 170),
            perg("proteccion", "Protección divina", TipoConsumible.PROTECCION, ALIADOS, 35, 50, 65, 16, 160),
            perg("cura_total", "Cura total", TipoConsumible.CURA_TOTAL, ALIADO, 75, 100, 100, 18, 180),
            perg("meteorito", "Meteorito", TipoConsumible.DANIO, ENEMIGOS, 100, 140, 200, 22, 250)
    );

    private static final List<Objeto> POCIONES_DISPONIBLES = List.of(
            pocion("roja", "Poción roja", 80, 0, 40),
            pocion("verde", "Poción verde", 0, 50, 40),
            pocion("azul", "Poción azul", 50, 30, 55)
    );
    private static final List<Objeto> CATALOGO = java.util.stream.Stream.of(
            EQUIPOS.values().stream().filter(Equipo::enTienda).map(equipo -> equipo.plantilla(Calidad.NORMAL)),
            POCIONES_DISPONIBLES.stream(),
            PERGAMINOS_DISPONIBLES.stream().map(pergamino -> pergamino.plantilla(PotenciaPergamino.NORMAL))
    ).flatMap(java.util.function.Function.identity()).toList();

    private CatalogoObjetos() {
    }

    public static List<Objeto> tienda() {
        return CATALOGO;
    }

    public static List<String> codigosPergaminos() {
        return PERGAMINOS_DISPONIBLES.stream().map(Pergamino::codigo).toList();
    }

    public static Objeto pergamino(String codigo, PotenciaPergamino potencia) {
        return PERGAMINOS_DISPONIBLES.stream().filter(pergamino -> pergamino.codigo().equals(codigo))
                .findFirst().orElseThrow().plantilla(potencia).nuevaUnidad(false);
    }

    public static Objeto equipo(String codigo, Calidad calidad, boolean inicial) {
        return EQUIPOS.get(codigo).plantilla(calidad).nuevaUnidad(inicial);
    }

    public static List<Objeto> inicial(ClaseHeroe clase) {
        var codigos = switch (clase) {
            case GUERRERO -> List.of("espada_basica", "pesada_basica");
            case MAGO, SANADOR -> List.of("vara_aprendiz", "tunica_aprendiz");
            case ARQUERO -> List.of("arco_corto", "ligera_basica");
            case PICARO -> List.of("daga", "ropa");
        };
        return codigos.stream().map(codigo -> equipo(codigo, Calidad.NORMAL, true)).toList();
    }

    private static Map<String, Equipo> equipos() {
        var equipos = new LinkedHashMap<String, Equipo>();
        List.of(
            arma("daga", "Daga mellada", "Daga", "Daga afilada", 3, 0, 2, 0, 0, 40, DAGA, true),
            arma("espada_basica", "Espada básica mellada", "Espada básica", "Espada básica afilada", 4, 0, 0, 0, 0, 50, ESPADA, true),
            arma("arco_corto", "Arco corto astillado", "Arco corto", "Arco corto templado", 4, 0, 0, 10, 0, 60, ARCO, true),
            arma("vara_aprendiz", "Vara de aprendiz agrietada", "Vara de aprendiz", "Vara de aprendiz reforzada", 0, 3, 0, 0, 12, 60, VARA, true),
            arma("garrote", "Garrote astillado", "Garrote", "Garrote ferrado", 7, 0, 0, 15, 0, 100, GARROTE, true),
            arma("arco_largo", "Arco largo torcido", "Arco largo", "Arco largo templado", 8, 0, 0, 15, 0, 130, ARCO, true),
            arma("espada_acero", "Espada de acero mellada", "Espada de acero", "Espada de acero bruñida", 9, 0, 0, 16, 0, 150, ESPADA, true),
            arma("vara_magica", "Vara mágica agrietada", "Vara mágica", "Vara mágica reforzada", 0, 8, 0, 0, 14, 150, VARA, true),
            arma("espada_pesada", "Espada pesada mellada", "Espada pesada", "Espada pesada forjada", 12, 0, 0, 18, 0, 200, ESPADA, true),
            arma("vara_avanzada", "Vara avanzada agrietada", "Vara avanzada", "Vara avanzada engastada", 0, 13, 0, 0, 22, 240, VARA, true),
            armadura("ropa", "Ropa ligera remendada", "Ropa ligera", "Ropa ligera bordada", 2, 0, 0, 2, 0, 0, 0, 30, true),
            armadura("capucha", "Capucha desgastada", "Capucha", "Capucha nueva", 2, 0, 0, 4, 0, 0, 0, 45, true),
            armadura("tunica_aprendiz", "Túnica de aprendiz raída", "Túnica de aprendiz", "Túnica de aprendiz impecable", 2, 0, 0, 0, 3, 0, 0, 50, true),
            armadura("ligera", "Armadura ligera rasgada", "Armadura ligera", "Armadura ligera remachada", 4, 0, 0, 0, 0, 8, 0, 65, true),
            armadura("tunica_mago", "Túnica de mago raída", "Túnica de mago", "Túnica de mago impecable", 3, 0, 0, 0, 6, 0, 12, 110, true),
            armadura("media", "Armadura media abollada", "Armadura media", "Armadura media ensamblada", 7, 0, -1, 0, 0, 12, 0, 120, true),
            armadura("botas", "Botas reforzadas gastadas", "Botas reforzadas", "Botas reforzadas ligeras", 4, 0, 3, 0, 0, 10, 0, 130, true),
            armadura("pesada", "Armadura pesada abollada", "Armadura pesada", "Armadura pesada bruñida", 12, 0, -3, 0, 0, 15, 0, 180, true),
            armadura("casco", "Casco reforzado abollado", "Casco reforzado", "Casco reforzado templado", 10, 3, 0, 0, 0, 16, 0, 190, true),
            armadura("pesada_basica", "Armadura pesada básica abollada", "Armadura pesada básica", "Armadura pesada básica bruñida", 8, 0, -3, 0, 0, 15, 0, 0, false),
            armadura("ligera_basica", "Armadura ligera básica rasgada", "Armadura ligera básica", "Armadura ligera básica remachada", 4, 0, 0, 0, 0, 8, 0, 0, false),
            arma("espada_esqueleto", "Espada oxidada", "Espada", "Espada afilada", 6, 0, 0, 12, 0, 80, ESPADA, false),
            armadura("escudo", "Escudo abollado", "Escudo", "Escudo reforzado", 6, 0, 0, 0, 0, 10, 0, 70, false),
            armadura("antigua", "Armadura antigua agrietada", "Armadura antigua", "Armadura antigua restaurada", 7, 2, -1, 0, 0, 12, 0, 100, false),
            arma("maldita", "Espada maldita mellada", "Espada maldita", "Espada maldita ritual", 14, 0, 0, 20, 0, 250, ESPADA, false),
            armadura("oscura", "Armadura oscura agrietada", "Armadura oscura", "Armadura oscura templada", 15, 0, -4, 0, 0, 20, 0, 300, false),
            armadura("casco_oscuro", "Casco oscuro abollado", "Casco oscuro", "Casco oscuro grabado", 10, 5, 0, 0, 0, 16, 0, 190, false)
        ).forEach(equipo -> equipos.put(equipo.codigo(), equipo));
        return Collections.unmodifiableMap(equipos);
    }

    private static Equipo arma(String codigo, String inferior, String nombre, String superior,
                               int fuerza, int inteligencia, int critico, int minFuerza, int minInteligencia,
                               int precio, Habilidad habilidad, boolean enTienda) {
        return new Equipo(codigo, inferior, nombre, superior, ARMAS,
                new Bonificaciones(fuerza, 0, inteligencia, 0, 0, critico, 0, 0),
                minFuerza, minInteligencia, precio, habilidad, enTienda);
    }

    private static Equipo armadura(String codigo, String inferior, String nombre, String superior,
                                   int defensa, int resistencia, int velocidad, int esquiva, int potencia,
                                   int fuerza, int inteligencia, int precio, boolean enTienda) {
        return new Equipo(codigo, inferior, nombre, superior, ARMADURAS,
                new Bonificaciones(0, defensa, 0, resistencia, velocidad, 0, esquiva, potencia),
                fuerza, inteligencia, precio, null, enTienda);
    }

    private static Habilidad habilidad(String id, String nombre, int pm, boolean magica, int potencia,
                                       int precision, int critico, int penetracion) {
        return new Habilidad(id, nombre, "Habilidad del arma equipada. Potencia adicional: " + potencia + ".",
                pm, ENEMIGO, magica ? TipoEfecto.DANIO_MAGICO : TipoEfecto.DANIO_FISICO,
                potencia, precision, critico, penetracion, null, 0);
    }

    private static Objeto pocion(String codigo, String nombre, int pv, int pm, int precio) {
        return new Objeto(null, codigo, nombre, POCIONES, null, null, false, Bonificaciones.NINGUNA, 0, 0, precio,
                (precio + 1) / 2, precio, null, new EfectoConsumible(TipoConsumible.POCION, ALIADO, pv, pm, 0));
    }

    private static Pergamino perg(String codigo, String nombre, TipoConsumible tipo, TipoObjetivo objetivo,
                                  int debil, int normal, int poderoso, int inteligencia, int precio) {
        return new Pergamino(codigo, nombre, tipo, objetivo, debil, normal, poderoso, inteligencia, precio);
    }
}
