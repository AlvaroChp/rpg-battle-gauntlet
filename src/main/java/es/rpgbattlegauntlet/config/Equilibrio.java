package es.rpgbattlegauntlet.config;

public final class Equilibrio {
    public static final int REDUCCION_ESCUDO = 65;
    public static final int BONO_MAXIMO_FURIA_MALDITA = 30;
    public static final int PV_POR_PUNTO = 12;
    public static final int PM_POR_PUNTO = 8;
    public static final int PV_AL_SUBIR = 30;
    public static final int PM_AL_SUBIR = 20;

    // La campaña ampliada acumula 2688 EXP antes del dragón y necesita 2750 para el nivel 12.
    private static final int EXP_ADICIONAL_RONDA_14 = 62;

    private Equilibrio() {
    }

    public static int experienciaSiguiente(int nivel) {
        return 75 + 35 * (nivel - 1);
    }

    public static int puntosAlSubir(int nivelAlcanzado) {
        return nivelAlcanzado == 12 ? 5 : 3;
    }

    public static int experienciaRonda(int ronda) {
        return 75 + 18 * (ronda - 1) + (ronda == 14 ? EXP_ADICIONAL_RONDA_14 : 0);
    }

    public static int oroRonda(int ronda, int turnos, int supervivientes, int consumibles) {
        return Math.max(60 + 13 * ronda,
                95 + 14 * ronda + 12 * supervivientes - 2 * turnos - 6 * consumibles);
    }
}
