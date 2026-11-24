package es.rpgbattlegauntlet.model;

import es.rpgbattlegauntlet.enums.TipoConsumible;
import es.rpgbattlegauntlet.enums.TipoObjetivo;

public record EfectoConsumible(TipoConsumible tipo, TipoObjetivo objetivo, int cantidad, int magia, int proteccion,
                              boolean incendiario) {
    public EfectoConsumible(TipoConsumible tipo, TipoObjetivo objetivo, int cantidad, int magia, int proteccion) {
        this(tipo, objetivo, cantidad, magia, proteccion, false);
    }
}
