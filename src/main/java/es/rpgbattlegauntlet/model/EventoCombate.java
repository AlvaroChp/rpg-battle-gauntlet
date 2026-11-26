package es.rpgbattlegauntlet.model;

import es.rpgbattlegauntlet.enums.EstadoTemporal;

import java.util.Map;
import java.util.List;
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
        Objeto objetoRobado,
        int curacionActor,
        CambioEstados cambioEstados,
        List<Ajuste> ajustesPrevios,
        List<Ajuste> ajustesPosteriores,
        String nombreHabilidad
) {
    public record Ajuste(UUID combatiente, int cambioPc, CambioEstados estados) {}

    EventoCombate conAjustes(List<Ajuste> ajustes, boolean previos) {
        return new EventoCombate(turno, actor, tipoActor, habilidad, objetivo, tipoObjetivo, danio, curacion,
                magiaRecuperada, esquivado, critico, objetoRobado, curacionActor, cambioEstados,
                previos ? ajustes : ajustesPrevios, previos ? ajustesPosteriores : ajustes, nombreHabilidad);
    }

    public record CambioEstados(Map<EstadoTemporal, Integer> antes, Map<EstadoTemporal, Integer> despues) {
        static CambioEstados entre(Map<EstadoTemporal, Integer> antes, Map<EstadoTemporal, Integer> despues) {
            return antes.equals(despues) ? null : new CambioEstados(antes, despues);
        }
    }

    public EventoCombate(int turno, UUID actor, String tipoActor, String habilidad, UUID objetivo, String tipoObjetivo,
                         int danio, int curacion, int magiaRecuperada, boolean esquivado, boolean critico,
                         Objeto objetoRobado, int curacionActor, CambioEstados cambioEstados) {
        this(turno, actor, tipoActor, habilidad, objetivo, tipoObjetivo, danio, curacion, magiaRecuperada,
                esquivado, critico, objetoRobado, curacionActor, cambioEstados, null);
    }

    public EventoCombate(int turno, UUID actor, String tipoActor, String habilidad, UUID objetivo, String tipoObjetivo,
                         int danio, int curacion, int magiaRecuperada, boolean esquivado, boolean critico,
                         Objeto objetoRobado, int curacionActor, CambioEstados cambioEstados, String nombreHabilidad) {
        this(turno, actor, tipoActor, habilidad, objetivo, tipoObjetivo, danio, curacion, magiaRecuperada,
                esquivado, critico, objetoRobado, curacionActor, cambioEstados, List.of(), List.of(), nombreHabilidad);
    }

    public EventoCombate(int turno, UUID actor, String tipoActor, String habilidad, UUID objetivo, String tipoObjetivo,
                         int danio, int curacion, int magiaRecuperada, boolean esquivado, boolean critico, Objeto objetoRobado) {
        this(turno, actor, tipoActor, habilidad, objetivo, tipoObjetivo, danio, curacion, magiaRecuperada,
                esquivado, critico, objetoRobado, 0, null);
    }
}
