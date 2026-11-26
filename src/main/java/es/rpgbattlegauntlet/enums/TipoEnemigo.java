package es.rpgbattlegauntlet.enums;

public enum TipoEnemigo {
    GOBLIN, GOBLIN_ARQUERO, ESQUELETO, CHAMAN, OGRO, CABALLERO_OSCURO, NIGROMANTE, GOLEM_RUNICO, VERDUGO_ESPECTRAL, DRAGON,
    DRACONIANO_CIMITARRA, DRACONIANO_BALLESTA, GUIVERNO, DRAGON_ESCARCHA, DRAGON_OBSIDIANA, DIOS_DRAGON;

    public boolean esDragon() {
        return switch (this) {
            case DRAGON, GUIVERNO, DRAGON_ESCARCHA, DRAGON_OBSIDIANA, DIOS_DRAGON -> true;
            default -> false;
        };
    }

    public boolean perteneceAlReinoDraconico() {
        return switch (this) {
            case DRACONIANO_CIMITARRA, DRACONIANO_BALLESTA, GUIVERNO, DRAGON_ESCARCHA, DRAGON_OBSIDIANA, DIOS_DRAGON -> true;
            default -> false;
        };
    }
}
