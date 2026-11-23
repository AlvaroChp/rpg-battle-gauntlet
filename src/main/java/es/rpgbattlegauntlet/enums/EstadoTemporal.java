package es.rpgbattlegauntlet.enums;

public enum EstadoTemporal {
    DEFENSA(40, 0, 0),
    GUARDIA(55, 0, 0),
    BENDICION(25, 0, 0),
    EVASION(0, 50, 0),
    RALENTIZACION(0, 0, -4),
    GUARDIA_OSEA_ACTIVA(45, 0, 0),
    GUARDIA_OSCURA_ACTIVA(55, 0, 0),
    CONTRAATAQUE_LISTO(0, 0, 0),
    DEFENSA_DEBILITADA(0, 0, 0),
    PROTECCION_35(35, 0, 0),
    PROTECCION_50(50, 0, 0),
    PROTECCION_65(65, 0, 0);

    private final int reduccionDanio;
    private final int esquivaAdicional;
    private final int cambioVelocidad;

    EstadoTemporal(int reduccionDanio, int esquivaAdicional, int cambioVelocidad) {
        this.reduccionDanio = reduccionDanio;
        this.esquivaAdicional = esquivaAdicional;
        this.cambioVelocidad = cambioVelocidad;
    }

    public int reduccionDanio() {
        return reduccionDanio;
    }

    public int esquivaAdicional() {
        return esquivaAdicional;
    }

    public int cambioVelocidad() {
        return cambioVelocidad;
    }
}
