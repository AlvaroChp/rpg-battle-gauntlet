package es.rpgbattlegauntlet.config;

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
    public static final Habilidad ATAQUE_DOBLE = new Habilidad("ataque_doble", "Ataque doble",
            "Dos golpes de daga al mismo objetivo.", 0, ENEMIGO, DANIO_FISICO, -9, 0, 0, 0, null, 0, 2);
    public static final Habilidad ESPADAZO = ataque("espadazo", "Espadazo", DANIO_FISICO, ENEMIGO, 0, 6, null);
    public static final Habilidad GUARDIA_OSEA = estado("guardia_osea", "Guardia ósea", PROPIO, 0, GUARDIA_OSEA_ACTIVA);
    public static final Habilidad BOLA_FUEGO = ataque("fuego_chaman", "Bola de fuego", DANIO_MAGICO, ENEMIGO, 10, 23, null);
    public static final Habilidad CURACION_OSCURA = new Habilidad("curacion_oscura", "Curación oscura",
            "Cura a un aliado vivo.", 14, ALIADO, CURACION, 16, 0, 0, 0, null, 0);
    public static final Habilidad MAZAZO = ataque("mazazo", "Mazazo", DANIO_FISICO, ENEMIGO, 0, 13, null);
    public static final Habilidad BARRIDO = ataque("barrido", "Ataque de área", DANIO_FISICO, ENEMIGOS, 0, -12, null);
    public static final Habilidad ESPADA_MALDITA = ataque("espada_maldita", "Espada maldita", DANIO_FISICO, ENEMIGO, 0, 8, DEFENSA_DEBILITADA);
    public static final Habilidad GUARDIA_OSCURA = estado("guardia_oscura", "Guardia oscura", PROPIO, 8, GUARDIA_OSCURA_ACTIVA);
    public static final Habilidad PREPARAR_CONTRAATAQUE = estado("preparar_contraataque", "Preparar contraataque", PROPIO, 6, CONTRAATAQUE_LISTO);
    public static final Habilidad CONTRAATAQUE = ataque("contraataque", "Contraataque", DANIO_FISICO, ENEMIGO, 0, -8, null);
    public static final Habilidad GARRA = ataque("garra", "Garra", DANIO_FISICO, ENEMIGO, 0, 10, null);
    public static final Habilidad ALIENTO = ataque("aliento_fuego", "Aliento de fuego", DANIO_MAGICO, ENEMIGOS, 20, 28, null);
    public static final Habilidad RUGIDO = estado("rugido", "Rugido", ENEMIGOS, 8, RALENTIZACION);

    private static final Map<TipoEnemigo, EstadisticasBase> ESTADISTICAS = Map.of(
            TipoEnemigo.GOBLIN, new EstadisticasBase(65, 0, 10, 5, 0, 4, 11, 5, 10),
            TipoEnemigo.ESQUELETO, new EstadisticasBase(90, 0, 12, 10, 0, 5, 7, 5, 5),
            TipoEnemigo.CHAMAN, new EstadisticasBase(70, 80, 5, 5, 14, 12, 9, 5, 5),
            TipoEnemigo.OGRO, new EstadisticasBase(220, 0, 20, 14, 0, 8, 4, 10, 0),
            TipoEnemigo.CABALLERO_OSCURO, new EstadisticasBase(240, 40, 20, 18, 10, 15, 10, 10, 5),
            TipoEnemigo.DRAGON, new EstadisticasBase(650, 120, 27, 20, 22, 18, 12, 15, 5)
    );

    private CatalogoEnemigos() {
    }

    public static Enemigo crear(TipoEnemigo tipo) {
        return new Enemigo(tipo, ESTADISTICAS.get(tipo));
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
}
