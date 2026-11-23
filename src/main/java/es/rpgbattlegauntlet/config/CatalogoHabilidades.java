package es.rpgbattlegauntlet.config;

import es.rpgbattlegauntlet.enums.ClaseHeroe;
import es.rpgbattlegauntlet.model.Habilidad;

import java.util.List;
import java.util.Map;

import static es.rpgbattlegauntlet.enums.EstadoTemporal.*;
import static es.rpgbattlegauntlet.enums.TipoEfecto.*;
import static es.rpgbattlegauntlet.enums.TipoObjetivo.*;

public final class CatalogoHabilidades {

    public static final Habilidad ATAQUE_BASICO = new Habilidad(
            "ataque_basico", "Ataque básico", "Inflige daño físico a un enemigo.",
            0, ENEMIGO, DANIO_FISICO, 0, 0, 0, 0, null, 0);

    public static final Habilidad DEFENDER = new Habilidad(
            "defender", "Defenderse", "Reduce el daño recibido un 40 % durante un turno.",
            0, PROPIO, ESTADO, 0, 0, 0, 0, DEFENSA, 1);

    private static final Map<ClaseHeroe, List<Habilidad>> POR_CLASE = Map.of(
            ClaseHeroe.GUERRERO, List.of(
                    new Habilidad("golpe_poderoso", "Golpe poderoso", "Daño físico contundente.",
                            5, ENEMIGO, DANIO_FISICO, 16, 0, 0, 0, null, 0),
                    new Habilidad("guardia", "Guardia", "Reduce el daño recibido por el guerrero un 55 % durante dos turnos.",
                            4, PROPIO, ESTADO, 0, 0, 0, 0, GUARDIA, 2)),
            ClaseHeroe.MAGO, List.of(
                    new Habilidad("bola_fuego", "Bola de fuego", "Inflige daño mágico a un enemigo.",
                            9, ENEMIGO, DANIO_MAGICO, 30, 0, 0, 0, null, 0),
                    new Habilidad("descarga_arcana", "Descarga arcana", "Inflige daño mágico a todos los enemigos.",
                            13, ENEMIGOS, DANIO_MAGICO, 14, 0, 0, 0, null, 0)),
            ClaseHeroe.ARQUERO, List.of(
                    new Habilidad("disparo_preciso", "Disparo preciso", "Aumenta la precisión e inflige daño físico.",
                            4, ENEMIGO, DANIO_FISICO, 8, 25, 0, 0, null, 0),
                    new Habilidad("flecha_perforante", "Flecha perforante", "Ignora un 50 % de la defensa física y ralentiza durante un turno.",
                            6, ENEMIGO, DANIO_FISICO, 7, 0, 0, 50, RALENTIZACION, 1)),
            ClaseHeroe.SANADOR, List.of(
                    new Habilidad("curacion", "Curación", "Recupera PV de un aliado vivo, incluido el sanador.",
                            8, ALIADO, CURACION, 34, 0, 0, 0, null, 0),
                    new Habilidad("bendicion", "Bendición", "Reduce el daño recibido por un aliado un 25 % durante dos turnos.",
                            6, ALIADO, ESTADO, 0, 0, 0, 0, BENDICION, 2)),
            ClaseHeroe.PICARO, List.of(
                    new Habilidad("ataque_furtivo", "Ataque furtivo", "Inflige daño físico con mayor probabilidad de crítico.",
                            5, ENEMIGO, DANIO_FISICO, 10, 0, 20, 0, null, 0),
                    new Habilidad("esquivar", "Esquivar", "Añade 50 puntos porcentuales de esquiva durante un turno.",
                            4, PROPIO, ESTADO, 0, 0, 0, 0, EVASION, 1))
    );

    private CatalogoHabilidades() {
    }

    public static List<Habilidad> deClase(ClaseHeroe clase) {
        return POR_CLASE.get(clase);
    }
}
