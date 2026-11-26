package es.rpgbattlegauntlet.model;

import es.rpgbattlegauntlet.enums.TipoConsumible;
import es.rpgbattlegauntlet.enums.TipoObjetivo;

public record EfectoConsumible(TipoConsumible tipo, TipoObjetivo objetivo, int cantidad, int magia, int proteccion,
                              boolean incendiario, boolean escarcha) {
    void validarRecuperacion(Combatiente destinatario) {
        var base = destinatario.getEstadisticasBase();
        if (tipo == TipoConsumible.POCION
                && !(cantidad > 0 && destinatario.getPvActuales() < base.pvMaximos())
                && !(magia > 0 && destinatario.getPmActuales() < base.pmMaximos())) {
            throw new IllegalArgumentException("Esta poción no recuperaría PV ni PC");
        }
    }

    public EfectoConsumible(TipoConsumible tipo, TipoObjetivo objetivo, int cantidad, int magia, int proteccion,
                            boolean incendiario) {
        this(tipo, objetivo, cantidad, magia, proteccion, incendiario, false);
    }
    public EfectoConsumible(TipoConsumible tipo, TipoObjetivo objetivo, int cantidad, int magia, int proteccion) {
        this(tipo, objetivo, cantidad, magia, proteccion, false);
    }
}
