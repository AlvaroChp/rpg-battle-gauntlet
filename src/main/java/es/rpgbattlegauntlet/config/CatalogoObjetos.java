package es.rpgbattlegauntlet.config;

import es.rpgbattlegauntlet.enums.*;
import es.rpgbattlegauntlet.model.*;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static es.rpgbattlegauntlet.enums.Categoria.*;
import static es.rpgbattlegauntlet.enums.TipoObjetivo.*;

public final class CatalogoObjetos {
    private record Equipo(String codigo, String inferior, String normal, String superior, Categoria categoria,
                          Bonificaciones bonos, int fuerza, int inteligencia, int precio, Habilidad habilidad, FamiliaArma familia,
                          boolean enTienda, RanuraEquipo ranura, boolean dosManos) {
        Objeto plantilla(Calidad calidad) {
            String nombre = switch (calidad) {
                case INFERIOR -> inferior;
                case NORMAL -> normal;
                case SUPERIOR -> superior;
            };
            return new Objeto(null, codigo, nombre, categoria, calidad, false, bonos.ajustar(calidad), fuerza,
                    inteligencia, precio, calidad.venta(precio), calidad.recompra(precio), habilidad, familia, null, ranura, dosManos);
        }
    }

    private record Pergamino(String codigo, String nombre, TipoConsumible tipo, TipoObjetivo objetivo,
                             int cantidad, int inteligencia, int precio, boolean incendiario) {
        Objeto plantilla() {
            int proteccion = tipo == TipoConsumible.PROTECCION ? cantidad : 0;
            int venta = (precio + 1) / 2;
            return new Objeto(null, codigo, nombre, PERGAMINOS, null, false, Bonificaciones.NINGUNA,
                    0, inteligencia, precio, venta, 2 * venta, null, null,
                    new EfectoConsumible(tipo, objetivo, cantidad, 0, proteccion, incendiario, codigo.equals("ventisca")), null, false);
        }
    }

    private static final Habilidad DAGA = habilidad("estocada_rapida", "Estocada rápida", 3, false, 5, 20, 5, 0);
    private static final Habilidad PUNTADA_TRAICIONERA = habilidad("puntada_traicionera", "Puntada traicionera", 5, false, 8, 10, 20, 0);
    private static final Habilidad CORTE_AL_TENDON = new Habilidad("corte_al_tendon", "Corte al tendón",
            "Inflige daño físico con +7 de potencia y reduce la velocidad del objetivo en 4 durante un turno.",
            4, ENEMIGO, TipoEfecto.DANIO_FISICO, 7, 0, 0, 0, EstadoTemporal.RALENTIZACION, 1);
    private static final Habilidad SANGRIA_RITUAL = new Habilidad("sangria_ritual", "Sangría ritual",
            "Inflige daño físico con +8 de potencia. Recupera el 50 % del daño infligido como PV.",
            5, ENEMIGO, TipoEfecto.DANIO_FISICO, 8, 0, 0, 0, null, 0, 1, 100, 50);
    private static final Habilidad CORTE_AMPLIO = habilidad("corte", "Corte amplio", 4, false, 10, 10, 0, 0);
    private static final Habilidad ESTOCADA_PRECISA = habilidad("estocada_precisa", "Estocada precisa", 4, false, 12, 30, 0, 0);
    private static final Habilidad TAJO_DEMOLEDOR = habilidad("tajo_demoledor", "Tajo demoledor", 6, false, 16, 0, 0, 30);
    private static final Habilidad CORTE_ULTRATUMBA = new Habilidad("corte_ultratumba", "Corte de ultratumba",
            "Inflige daño físico con +11 de potencia y ralentiza al objetivo durante un turno.",
            4, ENEMIGO, TipoEfecto.DANIO_FISICO, 11, 0, 0, 0, EstadoTemporal.RALENTIZACION, 1);
    private static final Habilidad FILO_MALDICION = new Habilidad("filo_maldicion", "Filo de la maldición",
            "Inflige daño físico con +14 de potencia y reduce la DEF del objetivo en 5 durante dos turnos.",
            6, ENEMIGO, TipoEfecto.DANIO_FISICO, 14, 0, 0, 0, EstadoTemporal.DEFENSA_DEBILITADA, 2);
    private static final Habilidad DISPARO_TENSO = habilidad("disparo_tenso", "Disparo tenso", 5, false, 10, 15, 0, 25);
    private static final Habilidad FLECHA_ENVENENADA = new Habilidad("flecha_envenenada", "Flecha envenenada",
            "Inflige daño físico con +4 de potencia y +10 de precisión. Si acierta, envenena con un 50 % de probabilidad, o un 25 % con Defenderse o Bendición.",
            4, ENEMIGO, TipoEfecto.DANIO_FISICO, 4, 10, 0, 0, EstadoTemporal.ENVENENADO, 2, 1, 50);
    private static final Habilidad FLECHA_DE_ALCANCE = habilidad("flecha_de_alcance", "Flecha de largo alcance", 5, false, 12, 15, 0, 30);
    private static final Habilidad BALLESTA = habilidad("virote_perforante", "Virote perforante", 6, false, 12, 10, 0, 35);
    private static final Habilidad PULSO = habilidad("pulso", "Pulso de vara", 5, true, 13, 0, 0, 0);
    private static final Habilidad DESTELLO_ARCANO = habilidad("destello_arcano", "Destello arcano", 4, true, 12, 0, 0, 0);
    private static final Habilidad ECO_DE_LA_TORMENTA = new Habilidad("eco_de_la_tormenta", "Eco de la tormenta",
            "Inflige daño mágico a todos los enemigos con +10 de potencia y reduce su velocidad en 4 durante un turno.",
            9, ENEMIGOS, TipoEfecto.DANIO_MAGICO, 10, 0, 0, 0, EstadoTemporal.RALENTIZACION, 1);
    private static final Habilidad DESCARGA_ASTRAL = habilidad("descarga_astral", "Descarga astral", 8, true, 22, 0, 0, 0);
    private static final Habilidad GARROTE = habilidad("impacto", "Impacto contundente", 5, false, 14, 0, 0, 15);
    private static final Habilidad ROMPEESCAMAS = habilidad("virote_rompeescamas", "Virote rompeescamas", 8, false, 20, 10, 0, 55);
    private static final Habilidad CORTE_ESCAMAS = CatalogoEnemigos.CORTE_ESCAMAS;
    private static final Habilidad MORDIDA_COLMILLO = habilidad("mordida_colmillo", "Mordida de colmillo", 6, false, 14, 15, 30, 0);
    private static final Habilidad ECO_DRACONICO = habilidad("eco_draconico", "Eco dracónico", 8, true, 27, 0, 0, 0);
    private static final Habilidad FILO_MATADRAGONES = new Habilidad("filo_matadragones", "Filo matadragones",
            "Inflige daño físico con +18 de potencia. Causa un 50 % más de daño contra dragones.",
            7, ENEMIGO, TipoEfecto.DANIO_FISICO, 18, 0, 0, 0, null, 0);
    private static final Habilidad CONGELACION = new Habilidad("congelacion", "Congelación",
            "Inflige daño mágico de hielo con +26 de potencia y aplica Escarcha durante un turno. Causa un 75 % más de daño contra dragones.",
            10, ENEMIGO, TipoEfecto.DANIO_MAGICO, 26, 0, 0, 0, EstadoTemporal.ESCARCHA, 1);
    private static final Habilidad PUNZADA_VENENOSA = new Habilidad("punzada_venenosa", "Punzada venenosa",
            "Inflige daño físico con +12 de potencia. Puede envenenar durante dos turnos con un 50 % de probabilidad, o un 25 % con Defenderse o Bendición.",
            5, ENEMIGO, TipoEfecto.DANIO_FISICO, 12, 0, 0, 0, EstadoTemporal.ENVENENADO, 2, 1, 50);

    private static final Habilidad DESCARGA_FUNEBRE = habilidad(
            "descarga_funebre", "Descarga fúnebre", 7, true, 12, 0, 0, 0);
    private static final Habilidad SENTENCIA_ESPECTRAL = habilidad(
            "sentencia_espectral", "Sentencia espectral", 6, false, 10, 0, 0, 25);

    private static final Map<String, Equipo> EQUIPOS = equipos();
    private static final List<Pergamino> PERGAMINOS_DISPONIBLES = List.of(
            perg("proyectil", "Proyectil mágico", TipoConsumible.DANIO, ENEMIGO, 65, 5, 65),
            perg("cura", "Cura", TipoConsumible.CURACION, ALIADO, 90, 6, 75),
            perg("cura_mayor", "Cura mayor", TipoConsumible.CURACION, ALIADO, 120, 8, 100),
            new Pergamino("llamarada", "Llamarada", TipoConsumible.DANIO, ENEMIGO, 100, 10, 110, true),
            perg("ventisca", "Ventisca", TipoConsumible.DANIO, ENEMIGOS, 65, 12, 140),
            perg("cura_grupal", "Cura grupal", TipoConsumible.CURACION, ALIADOS, 70, 14, 150),
            perg("tormenta", "Tormenta eléctrica", TipoConsumible.DANIO, ENEMIGOS, 80, 15, 170),
            perg("proteccion", "Protección divina", TipoConsumible.PROTECCION, ALIADOS, 50, 16, 160),
            perg("cura_total", "Cura total", TipoConsumible.CURA_TOTAL, ALIADO, 100, 18, 180),
            new Pergamino("meteorito", "Meteorito", TipoConsumible.DANIO, ENEMIGOS, 140, 22, 250, true)
    );

    private static final Objeto REVITALIZACION = pocion("revitalizacion", "Poción dorada", 120,
            new EfectoConsumible(TipoConsumible.REVITALIZACION, ALIADO_CAIDO, 25, 0, 0));
    private static final List<Objeto> POCIONES_DISPONIBLES = List.of(
            pocion("roja", "Poción roja", 80, 0, 40),
            pocion("verde", "Poción verde", 0, 50, 40),
            pocion("azul", "Poción azul", 50, 30, 55),
            REVITALIZACION
    );
    private static final List<Objeto> CATALOGO = java.util.stream.Stream.of(
            EQUIPOS.values().stream().filter(Equipo::enTienda).map(equipo -> equipo.plantilla(Calidad.NORMAL)),
            POCIONES_DISPONIBLES.stream(),
            PERGAMINOS_DISPONIBLES.stream().map(Pergamino::plantilla)
    ).flatMap(java.util.function.Function.identity()).toList();

    private CatalogoObjetos() {
    }

    public static List<Objeto> tienda() {
        return CATALOGO;
    }

    private static final List<String> EQUIPO_DRACONICO = List.of("cazadragones", "daga_colmillo", "vara_medula",
            "coraza_escamas", "jubon_guiverno", "tunica_escarcha");

    public static List<Objeto> tiendaDraconica() {
        return java.util.stream.Stream.concat(
                EQUIPO_DRACONICO.stream().map(codigo -> EQUIPOS.get(codigo).plantilla(Calidad.SUPERIOR)),
                CATALOGO.stream().filter(objeto -> objeto.efecto() != null)).toList();
    }

    public static boolean esEquipoDraconico(Objeto objeto) {
        return EQUIPO_DRACONICO.contains(objeto.codigo()) || objeto.codigo().equals("cimitarra_draconica")
                || objeto.codigo().equals("ballesta") && objeto.calidad() == Calidad.SUPERIOR;
    }

    public static Objeto pocionRevitalizacion() {
        return REVITALIZACION.nuevaUnidad(false);
    }

    public static Objeto regaloInicial(ClaseHeroe clase) {
        return switch (clase) {
            case SANADOR -> pergamino("cura_grupal");
            case MAGO -> pergamino("proyectil");
            case GUERRERO, PICARO, ARQUERO -> {
                String codigo = switch (clase) {
                    case GUERRERO -> "roja";
                    case PICARO -> "verde";
                    default -> "azul";
                };
                yield POCIONES_DISPONIBLES.stream().filter(pocion -> pocion.codigo().equals(codigo))
                        .findFirst().orElseThrow().nuevaUnidad(false);
            }
        };
    }

    public static List<String> codigosPergaminos() {
        return PERGAMINOS_DISPONIBLES.stream().map(Pergamino::codigo).toList();
    }

    public static Objeto pergamino(String codigo) {
        return PERGAMINOS_DISPONIBLES.stream().filter(pergamino -> pergamino.codigo().equals(codigo))
                .findFirst().orElseThrow().plantilla().nuevaUnidad(false);
    }

    public static Objeto requiemDelDragon() {
        return pergaminoDragon(false).nuevaUnidad(false);
    }

    public static Objeto pergaminoDragon(boolean aLaVenta) {
        return new Objeto(null, "requiem_dragon", "Pergamino de Dragón", PERGAMINOS, null, false,
                Bonificaciones.NINGUNA, 0, 22, aLaVenta ? 1000 : 0, 0, 0, null, null,
                new EfectoConsumible(TipoConsumible.ANIQUILAR_DRAGON, ENEMIGO, 0, 0, 0), null, false);
    }

    public static Objeto equipo(String codigo, Calidad calidad, boolean inicial) {
        return plantillaEquipo(codigo, calidad).nuevaUnidad(inicial);
    }

    public static Objeto plantillaEquipo(String codigo, Calidad calidad) {
        return EQUIPOS.get(codigo).plantilla(calidad);
    }

    public static Optional<Habilidad> habilidadArma(String codigo) {
        return Optional.ofNullable(EQUIPOS.get(codigo)).map(Equipo::habilidad);
    }

    public static List<Objeto> inicial(ClaseHeroe clase) {
        return codigosIniciales(clase).stream().map(codigo -> equipo(codigo, Calidad.NORMAL, true)).toList();
    }

    public static boolean esEquipoInicial(String codigo) {
        return java.util.Arrays.stream(ClaseHeroe.values()).anyMatch(clase -> codigosIniciales(clase).contains(codigo));
    }

    private static List<String> codigosIniciales(ClaseHeroe clase) {
        return switch (clase) {
            case GUERRERO -> List.of("espada_basica", "pesada_basica", "guantes", "botas_basicas");
            case MAGO, SANADOR -> List.of("vara_aprendiz", "tunica_aprendiz", "sombrero_mago", "botas_basicas");
            case ARQUERO -> List.of("arco_corto", "ligera_basica", "capucha", "botas_basicas");
            case PICARO -> List.of("daga", "ropa", "capucha", "botas_basicas");
        };
    }

    private static Map<String, Equipo> equipos() {
        var equipos = new LinkedHashMap<String, Equipo>();
        List.of(
            arma("daga", "Daga mellada", "Daga", "Daga afilada", 3, 0, 2, 0, 0, 40, FamiliaArma.DAGA, DAGA, true),
            arma("daga_duelo", "Daga de duelo mellada", "Daga de duelo", "Daga de duelo bruñida", 5, 0, 0, 12, 0, 95, FamiliaArma.DAGA, PUNTADA_TRAICIONERA, true),
            arma("daga_curva", "Daga curva astillada", "Daga curva", "Daga curva afilada", 6, 0, 0, 14, 0, 115, FamiliaArma.DAGA, CORTE_AL_TENDON, true),
            arma("daga_ritual", "Daga ritual mellada", "Daga ritual", "Daga ritual consagrada", 5, 0, 3, 12, 0, 110, FamiliaArma.DAGA, SANGRIA_RITUAL, false),
            arma("espada_basica", "Espada básica mellada", "Espada básica", "Espada básica afilada", 4, 0, 0, 0, 0, 50, FamiliaArma.ESPADA, CORTE_AMPLIO, true),
            arma("arco_corto", "Arco corto astillado", "Arco corto", "Arco corto templado", 4, 0, 0, 10, 0, 60, FamiliaArma.ARCO, DISPARO_TENSO, true),
            arma("arco_goblin", "Arco goblin astillado", "Arco goblin", "Arco goblin reforzado", 5, 0, 0, 10, 0, 75, FamiliaArma.ARCO, FLECHA_ENVENENADA, false),
            arma("vara_aprendiz", "Vara de aprendiz agrietada", "Vara de aprendiz", "Vara de aprendiz reforzada", 0, 3, 0, 0, 12, 60, FamiliaArma.VARA, PULSO, true),
            arma("garrote", "Garrote astillado", "Garrote", "Garrote ferrado", 7, 0, 0, 15, 0, 100, FamiliaArma.GARROTE, GARROTE, true),
            arma("arco_largo", "Arco largo torcido", "Arco largo", "Arco largo templado", 8, 0, 0, 15, 0, 130, FamiliaArma.ARCO, FLECHA_DE_ALCANCE, true),
            arma("ballesta", "Ballesta desgastada", "Ballesta", "Ballesta dracónica", 10, 0, 0, 16, 0, 175, FamiliaArma.BALLESTA, BALLESTA, true),
            arma("cazadragones", "Ballesta Cazadragones gastada", "Ballesta Cazadragones", "Ballesta Cazadragones",
                    17, 0, 0, 20, 0, 560, FamiliaArma.BALLESTA, ROMPEESCAMAS, false),
            arma("cimitarra_draconica", "Cimitarra dracónica mellada", "Cimitarra dracónica", "Cimitarra dracónica bruñida",
                    14, 0, 0, 18, 0, 360, FamiliaArma.ESPADA, CORTE_ESCAMAS, false),
            arma("espada_obsidiana", "Espada de obsidiana mellada", "Espada de obsidiana", "Espada de obsidiana bruñida",
                    17, 0, 0, 20, 0, 540, FamiliaArma.ESPADA, FILO_MATADRAGONES, false),
            armadura("armadura_obsidiana", "Armadura de obsidiana agrietada", "Armadura de obsidiana", "Armadura de obsidiana templada",
                    20, 4, -4, 0, 0, 20, 0, 560, false),
            arma("cetro_invierno", "Cetro del Invierno agrietado", "Cetro del Invierno", "Cetro del Invierno engastado",
                    0, 19, 0, 0, 22, 520, FamiliaArma.VARA, CONGELACION, false),
            armadura("armadura_escarcha", "Armadura de escarcha agrietada", "Armadura de escarcha", "Armadura de escarcha templada",
                    13, 8, -2, 0, 0, 16, 0, 490, false),
            arma("aguijon_riscos", "Aguijón de los Riscos astillado", "Aguijón de los Riscos", "Aguijón de los Riscos afilado",
                    13, 0, 6, 16, 0, 400, FamiliaArma.DAGA, PUNZADA_VENENOSA, false),
            arma("daga_colmillo", "Daga de colmillo desgastada", "Daga de colmillo", "Daga de colmillo",
                    12, 0, 8, 16, 0, 430, FamiliaArma.DAGA, MORDIDA_COLMILLO, false),
            arma("vara_medula", "Vara de médula dracónica agrietada", "Vara de médula dracónica", "Vara de médula dracónica",
                    0, 17, 0, 0, 24, 495, FamiliaArma.VARA, ECO_DRACONICO, false),
            armadura("coraza_escamas", "Coraza de escamas agrietada", "Coraza de escamas", "Coraza de escamas",
                    19, 4, -4, 0, 0, 20, 0, 470, false),
            armadura("jubon_guiverno", "Jubón de piel de guiverno raído", "Jubón de piel de guiverno", "Jubón de piel de guiverno",
                    10, 4, 1, 5, 0, 12, 0, 400, false),
            armadura("tunica_escarcha", "Túnica de escarcha raída", "Túnica de escarcha", "Túnica de escarcha",
                    7, 10, 0, 0, 11, 0, 21, 445, false),
            arma("espada_acero", "Espada de acero mellada", "Espada de acero", "Espada de acero bruñida", 9, 0, 0, 16, 0, 150, FamiliaArma.ESPADA, ESTOCADA_PRECISA, true),
            arma("vara_magica", "Vara mágica agrietada", "Vara mágica", "Vara mágica reforzada", 0, 8, 0, 0, 14, 150, FamiliaArma.VARA, DESTELLO_ARCANO, true),
            arma("vara_chaman", "Vara de chamán agrietada", "Vara de chamán", "Vara de chamán ritual", 0, 9, 0, 0, 16, 170, FamiliaArma.VARA, ECO_DE_LA_TORMENTA, false),
            arma("espada_pesada", "Espada pesada mellada", "Espada pesada", "Espada pesada forjada", 12, 0, 0, 18, 0, 200, FamiliaArma.ESPADA, TAJO_DEMOLEDOR, true),
            arma("vara_avanzada", "Vara avanzada agrietada", "Vara avanzada", "Vara avanzada engastada", 0, 13, 0, 0, 22, 240, FamiliaArma.VARA, DESCARGA_ASTRAL, true),
            armadura("ropa", "Ropa ligera remendada", "Ropa ligera", "Ropa ligera bordada", 2, 0, 0, 2, 0, 0, 0, 30, true),
            armadura("guantes", "Guantes de cuero remendados", "Guantes de cuero", "Guantes de cuero curtido", 1, 1, 0, 0, 0, 0, 0, 45, true),
            armadura("guantes_reforzados", "Guantes reforzados abollados", "Guantes reforzados", "Guantes reforzados bruñidos", 2, 0, 0, 0, 0, 14, 0, 75, true),
            armadura("capucha", "Capucha desgastada", "Capucha", "Capucha nueva", 1, 0, 0, 2, 0, 0, 0, 45, true),
            armadura("sombrero_mago", "Sombrero de mago raído", "Sombrero de mago", "Sombrero de mago bordado",
                    0, 1, 0, 0, 2, 0, 12, 80, true),
            armadura("tunica_aprendiz", "Túnica de aprendiz raída", "Túnica de aprendiz", "Túnica de aprendiz impecable", 2, 0, 0, 0, 3, 0, 0, 50, true),
            armadura("ligera", "Armadura ligera rasgada", "Armadura ligera", "Armadura ligera remachada", 4, 0, 0, 0, 0, 8, 0, 65, true),
            armadura("tunica_mago", "Túnica de mago raída", "Túnica de mago", "Túnica de mago impecable", 3, 0, 0, 0, 6, 0, 12, 110, true),
            armadura("tunica_chaman", "Túnica de chamán raída", "Túnica de chamán", "Túnica de chamán ritual", 4, 2, 0, 0, 7, 0, 16, 145, false),
            armadura("media", "Armadura media abollada", "Armadura media", "Armadura media ensamblada", 7, 0, -1, 0, 0, 12, 0, 120, true),
            armadura("botas", "Botas reforzadas gastadas", "Botas reforzadas", "Botas reforzadas ligeras", 1, 0, 2, 0, 0, 10, 0, 130, true),
            armadura("botas_basicas", "Botas gastadas", "Botas", "Botas cuidadas", 1, 0, 0, 0, 0, 0, 0, 20, false),
            armadura("pesada", "Armadura pesada abollada", "Armadura pesada", "Armadura pesada bruñida", 12, 0, -3, 0, 0, 16, 0, 180, true),
            armadura("casco", "Casco reforzado abollado", "Casco reforzado", "Casco reforzado templado", 3, 1, 0, 0, 0, 16, 0, 190, true),
            armadura("pesada_basica", "Armadura pesada básica abollada", "Armadura pesada básica", "Armadura pesada básica bruñida", 8, 0, -3, 0, 0, 15, 0, 100, false),
            armadura("ligera_basica", "Armadura ligera básica rasgada", "Armadura ligera básica", "Armadura ligera básica remachada", 4, 0, 0, 0, 0, 8, 0, 0, false),
            arma("espada_esqueleto", "Espada antigua oxidada", "Espada antigua", "Espada antigua afilada",
                    6, 0, 0, 12, 0, 80, FamiliaArma.ESPADA, CORTE_ULTRATUMBA, false),
            armadura("escudo", "Escudo abollado", "Escudo", "Escudo reforzado", 2, 0, 0, 0, 0, 10, 0, 70, false),
            armadura("antigua", "Armadura antigua agrietada", "Armadura antigua", "Armadura antigua restaurada", 7, 2, -1, 0, 0, 12, 0, 100, false),
            arma("maldita", "Espada maldita mellada", "Espada maldita", "Espada maldita ritual", 14, 0, 0, 20, 0, 250, FamiliaArma.ESPADA, FILO_MALDICION, false),
            armadura("oscura", "Armadura oscura agrietada", "Armadura oscura", "Armadura oscura templada", 15, 0, -4, 0, 0, 20, 0, 300, false),
            armadura("casco_oscuro", "Casco oscuro abollado", "Casco oscuro", "Casco oscuro grabado", 3, 2, 0, 0, 0, 16, 0, 190, false),
            arma("vara_osario", "Vara de osario agrietada", "Vara de osario", "Vara de osario engastada",
                    0, 10, 0, 0, 18, 200, FamiliaArma.VARA, DESCARGA_FUNEBRE, false),
            armadura("capucha_funeraria", "Capucha funeraria raída", "Capucha funeraria", "Capucha funeraria bordada",
                    1, 2, 0, 0, 0, 0, 0, 75, false),
            armadura("coraza_runica", "Coraza rúnica agrietada", "Coraza rúnica", "Coraza rúnica restaurada",
                    13, 0, -4, 0, 0, 20, 0, 220, false),
            arma("hacha_espectral", "Hacha espectral mellada", "Hacha espectral", "Hacha espectral afilada",
                    11, 0, 0, 18, 0, 210, FamiliaArma.HACHA, SENTENCIA_ESPECTRAL, false),
            armadura("yelmo_espectral", "Yelmo espectral abollado", "Yelmo espectral", "Yelmo espectral pulido",
                    2, 0, 0, 2, 0, 14, 0, 105, false)
        ).forEach(equipo -> equipos.put(equipo.codigo(), equipo));
        return Collections.unmodifiableMap(equipos);
    }

    private static Equipo arma(String codigo, String inferior, String nombre, String superior,
                               int fuerza, int inteligencia, int critico, int minFuerza, int minInteligencia,
                               int precio, FamiliaArma familia, Habilidad habilidad, boolean enTienda) {
        return new Equipo(codigo, inferior, nombre, superior, ARMAS,
                new Bonificaciones(fuerza, 0, inteligencia, 0, 0, critico, 0, 0),
                minFuerza, minInteligencia, precio, habilidad, familia, enTienda, RanuraEquipo.ARMAS,
                switch (familia) {
                    case ARCO, BALLESTA, VARA, GARROTE, HACHA -> true;
                    case ESPADA -> codigo.equals("espada_pesada") || codigo.equals("maldita");
                    case DAGA -> false;
                });
    }

    private static Equipo armadura(String codigo, String inferior, String nombre, String superior,
                                   int defensa, int resistencia, int velocidad, int esquiva, int potencia,
                                   int fuerza, int inteligencia, int precio, boolean enTienda) {
        return new Equipo(codigo, inferior, nombre, superior, ARMADURAS,
                new Bonificaciones(0, defensa, 0, resistencia, velocidad, 0, esquiva, potencia),
                fuerza, inteligencia, precio, null, null, enTienda,
                switch (codigo) {
                    case "guantes", "guantes_reforzados" -> RanuraEquipo.GUANTES;
                    case "botas", "botas_basicas" -> RanuraEquipo.BOTAS;
                    case "capucha", "casco", "casco_oscuro", "capucha_funeraria", "yelmo_espectral", "sombrero_mago" -> RanuraEquipo.CABEZA;
                    case "escudo" -> RanuraEquipo.ESCUDO;
                    default -> RanuraEquipo.ARMADURAS;
                }, false);
    }

    private static Habilidad habilidad(String id, String nombre, int pm, boolean magica, int potencia,
                                       int precision, int critico, int penetracion) {
        String descripcion = "Daño " + (magica ? "mágico" : "físico") + " con +" + potencia + " de potencia."
                + (precision > 0 ? " Reduce la esquiva en " + precision + " puntos porcentuales." : "")
                + (critico > 0 ? " Añade " + critico + " puntos porcentuales de crítico." : "")
                + (penetracion > 0 ? " Ignora el " + penetracion + " % de la defensa." : "");
        return new Habilidad(id, nombre, descripcion,
                pm, ENEMIGO, magica ? TipoEfecto.DANIO_MAGICO : TipoEfecto.DANIO_FISICO,
                potencia, precision, critico, penetracion, null, 0);
    }

    private static Objeto pocion(String codigo, String nombre, int pv, int pm, int precio) {
        return pocion(codigo, nombre, precio, new EfectoConsumible(TipoConsumible.POCION, ALIADO, pv, pm, 0));
    }

    private static Objeto pocion(String codigo, String nombre, int precio, EfectoConsumible efecto) {
        return new Objeto(null, codigo, nombre, POCIONES, null, false, Bonificaciones.NINGUNA, 0, 0, precio,
                (precio + 1) / 2, precio, null, null, efecto, null, false);
    }

    private static Pergamino perg(String codigo, String nombre, TipoConsumible tipo, TipoObjetivo objetivo,
                                  int cantidad, int inteligencia, int precio) {
        return new Pergamino(codigo, nombre, tipo, objetivo, cantidad, inteligencia, precio, false);
    }
}
