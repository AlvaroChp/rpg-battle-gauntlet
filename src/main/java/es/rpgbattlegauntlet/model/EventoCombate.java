package es.rpgbattlegauntlet.model;

import java.util.UUID;

public record EventoCombate(
        int turno,
        UUID actor,
        String tipoActor,
        String habilidad,
        UUID objetivo,
        String tipoObjetivo,
        int danio,
        int curacion,
        int magiaRecuperada,
        boolean esquivado,
        boolean critico,
        Objeto objetoRobado
) {
}
