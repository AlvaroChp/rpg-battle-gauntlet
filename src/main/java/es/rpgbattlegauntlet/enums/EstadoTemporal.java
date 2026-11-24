package es.rpgbattlegauntlet.enums;

public enum EstadoTemporal {
    DEFENSA(40, 0, 0),
    INTERCEPTANDO(0, 0, 0),
    BENDICION(25, 0, 0),
    EVASION(0, 50, 0),
    RALENTIZACION(0, 0, -4),
    ENVENENADO(0, 0, 0, 4, "veneno"),
    QUEMADO(0, 0, 0, 5, "quemadura"),
    CARGA_FURIA_ANIMAL(0, 0, 0),
    CARGA_INCENDIO_CENIZA(0, 0, 0),
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
    private final int danioPeriodico;
    private final String eventoDanio;

    EstadoTemporal(int reduccionDanio, int esquivaAdicional, int cambioVelocidad) {
        this(reduccionDanio, esquivaAdicional, cambioVelocidad, 0, null);
    }

    EstadoTemporal(int reduccionDanio, int esquivaAdicional, int cambioVelocidad, int danioPeriodico, String eventoDanio) {
        this.reduccionDanio = reduccionDanio;
        this.esquivaAdicional = esquivaAdicional;
        this.cambioVelocidad = cambioVelocidad;
        this.danioPeriodico = danioPeriodico;
        this.eventoDanio = eventoDanio;
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

    public int danioPeriodico() {
        return danioPeriodico;
    }

    public String eventoDanio() {
        return eventoDanio;
    }
}
