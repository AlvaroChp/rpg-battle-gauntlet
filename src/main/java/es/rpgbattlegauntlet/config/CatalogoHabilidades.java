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
            "defender", "Defenderse", "Durante un turno, reduce el daño físico un 40 %, o un 65 % con escudo. Magos, sanadores y portadores de un escudo mágico reducen también el daño mágico un 40 %.",
            0, PROPIO, ESTADO, 0, 0, 0, 0, DEFENSA, 1);

    private static final Map<ClaseHeroe, List<Habilidad>> POR_CLASE = Map.of(
            ClaseHeroe.GUERRERO, List.of(
                    new Habilidad("golpe_poderoso", "Golpe poderoso", "Daño físico contundente.",
                            5, ENEMIGO, DANIO_FISICO, 16, 0, 0, 0, null, 0),
                    new Habilidad("interceptar", "Interceptar",
                            "Hasta tu próximo turno, recibe un golpe individual dirigido a un aliado o protege el primer golpe que te alcance. Reduce ese daño un 40 %, o un 65 % con escudo.",
                            4, PROPIO, ESTADO, 0, 0, 0, 0, INTERCEPTANDO, 1)),
            ClaseHeroe.MAGO, List.of(
                    new Habilidad("bola_fuego", "Bola de fuego", "Inflige daño mágico. Quema con un 50 % de probabilidad, o un 25 % con Defenderse o Bendición.",
                            9, ENEMIGO, DANIO_MAGICO, 30, 0, 0, 0, QUEMADO, 2, 1, 50),
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
                    new Habilidad("robar", "Robar", "Intenta robar un objeto al enemigo. La destreza aumenta la probabilidad.",
                            5, ENEMIGO, ROBO, 0, 0, 0, 0, null, 0),
                    new Habilidad("esquivar", "Esquivar", "Añade 50 puntos porcentuales de esquiva durante un turno.",
                            4, PROPIO, ESTADO, 0, 0, 0, 0, EVASION, 1))
    );

    private CatalogoHabilidades() {
    }

    public static List<Habilidad> deClase(ClaseHeroe clase) {
        return POR_CLASE.get(clase);
    }
}
