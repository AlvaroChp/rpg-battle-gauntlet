package es.rpgbattlegauntlet.model;

import es.rpgbattlegauntlet.enums.EstadoTemporal;
import es.rpgbattlegauntlet.enums.TipoEfecto;
import es.rpgbattlegauntlet.enums.TipoObjetivo;

public record Habilidad(
        String id,
        String nombre,
        String descripcion,
        int costePm,
        TipoObjetivo objetivo,
        TipoEfecto efecto,
        int potencia,
        int precisionAdicional,
        int criticoAdicional,
        int penetracion,
        EstadoTemporal estado,
        int duracion,
        int golpes,
        int probabilidadEstado
) {
    public Habilidad(String id, String nombre, String descripcion, int costePm, TipoObjetivo objetivo,
                     TipoEfecto efecto, int potencia, int precisionAdicional, int criticoAdicional,
                     int penetracion, EstadoTemporal estado, int duracion) {
        this(id, nombre, descripcion, costePm, objetivo, efecto, potencia, precisionAdicional,
                criticoAdicional, penetracion, estado, duracion, 1);
    }

    public Habilidad(String id, String nombre, String descripcion, int costePm, TipoObjetivo objetivo,
                     TipoEfecto efecto, int potencia, int precisionAdicional, int criticoAdicional,
                     int penetracion, EstadoTemporal estado, int duracion, int golpes) {
        this(id, nombre, descripcion, costePm, objetivo, efecto, potencia, precisionAdicional,
                criticoAdicional, penetracion, estado, duracion, golpes, 100);
    }
}
