package es.rpgbattlegauntlet.model;

import es.rpgbattlegauntlet.enums.TipoEnemigo;

import java.util.Objects;

public final class Enemigo extends Combatiente {

    private final TipoEnemigo tipo;
    private int accionesRealizadas;

    public Enemigo(TipoEnemigo tipo, EstadisticasBase estadisticasBase) {
        super(estadisticasBase);
        this.tipo = Objects.requireNonNull(tipo);
    }

    public TipoEnemigo getTipo() {
        return tipo;
    }

    public int getAccionesRealizadas() {
        return accionesRealizadas;
    }

    void registrarAccion() {
        accionesRealizadas++;
    }
}
