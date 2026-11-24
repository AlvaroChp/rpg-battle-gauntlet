package es.rpgbattlegauntlet.config;

import es.rpgbattlegauntlet.enums.Dificultad;
import es.rpgbattlegauntlet.enums.EstadoTemporal;
import es.rpgbattlegauntlet.enums.TipoEfecto;
import es.rpgbattlegauntlet.enums.TipoEnemigo;
import es.rpgbattlegauntlet.enums.TipoObjetivo;
import es.rpgbattlegauntlet.model.Enemigo;
import es.rpgbattlegauntlet.model.EstadisticasBase;
import es.rpgbattlegauntlet.model.Habilidad;

import java.util.Map;

import static es.rpgbattlegauntlet.enums.EstadoTemporal.*;
import static es.rpgbattlegauntlet.enums.TipoEfecto.*;
import static es.rpgbattlegauntlet.enums.TipoObjetivo.*;

public final class CatalogoEnemigos {

    public static final Habilidad CUCHILLADA = ataque("cuchillada", "Cuchillada", DANIO_FISICO, ENEMIGO, 0, 4, null);
    public static final Habilidad FLECHAZO_GOBLIN = ataque("flechazo_goblin", "Flechazo goblin", DANIO_FISICO, ENEMIGO, 0, 2, null);
    public static final Habilidad ATAQUE_DOBLE = new Habilidad("ataque_doble", "Ataque doble",
            "Dos golpes de daga al mismo objetivo.", 4, ENEMIGO, DANIO_FISICO, -9, 0, 0, 0, null, 0, 2);
    public static final Habilidad ESPADAZO = ataque("espadazo", "Espadazo", DANIO_FISICO, ENEMIGO, 0, 6, null);
    public static final Habilidad GUARDIA_OSEA = estado("guardia_osea", "Guardia ósea", PROPIO, 4, GUARDIA_OSEA_ACTIVA);
    public static final Habilidad BOLA_FUEGO = ataqueDeFuego("fuego_chaman", "Bola de fuego", ENEMIGO, 10, 23, 50);
    public static final Habilidad CURACION_OSCURA = new Habilidad("curacion_oscura", "Curación oscura",
            "Cura a un aliado vivo.", 14, ALIADO, CURACION, 16, 0, 0, 0, null, 0);
    public static final Habilidad MAZAZO = ataque("mazazo", "Mazazo", DANIO_FISICO, ENEMIGO, 0, 13, null);
    public static final Habilidad BARRIDO = ataque("barrido", "Ataque de área", DANIO_FISICO, ENEMIGOS, 6, -12, null);
    public static final Habilidad PREPARAR_FURIA_ANIMAL = estado("preparar_furia_animal", "Prepara Furia animal", PROPIO, 0, CARGA_FURIA_ANIMAL);
    public static final Habilidad FURIA_ANIMAL = ataque("furia_animal", "Furia animal", DANIO_FISICO, ENEMIGOS, 0, 4, null);
    public static final Habilidad ESPADA_MALDITA = ataque("espada_maldita", "Espada maldita", DANIO_FISICO, ENEMIGO, 0, 8, DEFENSA_DEBILITADA);
    public static final Habilidad GUARDIA_OSCURA = estado("guardia_oscura", "Guardia oscura", PROPIO, 8, GUARDIA_OSCURA_ACTIVA);
    public static final Habilidad PREPARAR_CONTRAATAQUE = estado("preparar_contraataque", "Preparar contraataque", PROPIO, 6, CONTRAATAQUE_LISTO);
    public static final Habilidad CONTRAATAQUE = ataque("contraataque", "Contraataque", DANIO_FISICO, ENEMIGO, 0, -8, null);
    public static final Habilidad DESCARGA_FUNEBRE = ataque(
            "descarga_funebre", "Descarga fúnebre", DANIO_MAGICO, ENEMIGO, 10, 12, null);
    public static final Habilidad REANIMAR = new Habilidad("reanimar", "Reanimar",
            "Levanta a un aliado caído con el 25 % de sus PV máximos una vez por combate.",
            18, ALIADO_CAIDO, REVIVIR, 25, 0, 0, 0, null, 0);
    public static final Habilidad PUNO_DE_PIEDRA = ataque(
            "puno_de_piedra", "Puño de piedra", DANIO_FISICO, ENEMIGO, 0, 5, null);
    public static final Habilidad PREPARAR_ONDA_RUNICA = estado(
            "preparar_onda_runica", "Prepara Onda rúnica", PROPIO, 0, CARGA_ONDA_RUNICA);
    public static final Habilidad ONDA_RUNICA = ataque(
            "onda_runica", "Onda rúnica", DANIO_FISICO, ENEMIGOS, 0, -18, null);
    public static final Habilidad TAJO_ESPECTRAL = ataque(
            "tajo_espectral", "Tajo espectral", DANIO_FISICO, ENEMIGO, 0, 3, null);
    public static final Habilidad MARCAR_SENTENCIA = new Habilidad(
            "marcar_sentencia", "Marca espectral", "Marca a un héroe para atacarlo en el siguiente turno del verdugo.",
            0, ENEMIGO, MARCAR, 0, 0, 0, 0, null, 0);
    public static final Habilidad SENTENCIA = ataque(
            "sentencia_espectral", "Sentencia espectral", DANIO_FISICO, ENEMIGO, 0, 15, null);
    public static final Habilidad BARRIDO_ESPECTRAL = ataque(
            "barrido_espectral", "Barrido espectral", DANIO_FISICO, ENEMIGOS, 0, -12, null);
    public static final Habilidad GARRA = ataque("garra", "Garra", DANIO_FISICO, ENEMIGO, 0, 10, null);
    public static final Habilidad ALIENTO = ataqueDeFuego("aliento_fuego", "Aliento de fuego", ENEMIGOS, 20, 28, 50);
    public static final Habilidad PREPARAR_INCENDIO_CENIZA = estado("preparar_incendio_ceniza", "Prepara Incendio de ceniza", PROPIO, 0, CARGA_INCENDIO_CENIZA);
    public static final Habilidad INCENDIO_CENIZA = ataqueDeFuego("incendio_ceniza", "Incendio de ceniza", ENEMIGOS, 0, 40, 100);
    public static final Habilidad RUGIDO = estado("rugido", "Rugido", ENEMIGOS, 8, RALENTIZACION);

    private static final Map<TipoEnemigo, EstadisticasBase> ESTADISTICAS = Map.of(
            TipoEnemigo.GOBLIN, new EstadisticasBase(65, 12, 10, 5, 0, 4, 11, 5, 10),
            TipoEnemigo.GOBLIN_ARQUERO, new EstadisticasBase(55, 16, 9, 3, 0, 4, 12, 8, 10),
            TipoEnemigo.ESQUELETO, new EstadisticasBase(90, 16, 12, 10, 0, 5, 7, 5, 5),
            TipoEnemigo.CHAMAN, new EstadisticasBase(70, 80, 5, 5, 14, 12, 9, 5, 5),
            TipoEnemigo.OGRO, new EstadisticasBase(220, 24, 20, 14, 0, 8, 4, 10, 0),
            TipoEnemigo.CABALLERO_OSCURO, new EstadisticasBase(240, 40, 20, 18, 10, 15, 10, 10, 5),
            TipoEnemigo.NIGROMANTE, new EstadisticasBase(125, 58, 6, 8, 19, 15, 8, 5, 5),
            TipoEnemigo.GOLEM_RUNICO, new EstadisticasBase(280, 0, 20, 27, 0, 6, 4, 5, 0),
            TipoEnemigo.VERDUGO_ESPECTRAL, new EstadisticasBase(185, 0, 20, 13, 0, 12, 9, 8, 8),
            TipoEnemigo.DRAGON, new EstadisticasBase(650, 120, 27, 20, 22, 18, 12, 15, 5)
    );

    private CatalogoEnemigos() {
    }

    public static Enemigo crear(TipoEnemigo tipo) {
        return crear(tipo, Dificultad.FACIL);
    }

    public static Enemigo crear(TipoEnemigo tipo, Dificultad dificultad) {
        var base = ESTADISTICAS.get(tipo);
        var ajustada = new EstadisticasBase(dificultad.vidaEnemiga(base.pvMaximos()), base.pmMaximos(), base.fuerza(),
                base.defensa(), base.inteligencia(), base.resistencia(), base.velocidad(), base.critico(), base.esquiva());
        return new Enemigo(tipo, ajustada, dificultad.multiplicadorDanioEnemigo());
    }

    private static Habilidad ataque(String id, String nombre, TipoEfecto efecto, TipoObjetivo objetivo,
                                    int pm, int potencia, EstadoTemporal estado) {
        return new Habilidad(id, nombre, "Inflige daño.", pm, objetivo, efecto, potencia,
                0, 0, 0, estado, estado == null ? 0 : 2);
    }

    private static Habilidad estado(String id, String nombre, TipoObjetivo objetivo, int pm, EstadoTemporal estado) {
        return new Habilidad(id, nombre, "Aplica " + nombre.toLowerCase(java.util.Locale.ROOT) + " durante un turno.",
                pm, objetivo, ESTADO, 0, 0, 0, 0, estado, 1);
    }

    private static Habilidad ataqueDeFuego(String id, String nombre, TipoObjetivo objetivo, int pc, int potencia, int probabilidad) {
        return new Habilidad(id, nombre, "Inflige daño mágico y puede causar quemaduras.", pc, objetivo,
                DANIO_MAGICO, potencia, 0, 0, 0, QUEMADO, 2, 1, probabilidad);
    }
}
