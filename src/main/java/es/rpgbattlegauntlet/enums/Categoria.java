package es.rpgbattlegauntlet.enums;

public enum Categoria {
    ARMAS, ARMADURAS, POCIONES, PERGAMINOS;

    public boolean esEquipo() {
        return this == ARMAS || this == ARMADURAS;
    }
}
