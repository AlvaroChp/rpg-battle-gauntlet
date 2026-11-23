package es.rpgbattlegauntlet.config;

public final class Equilibrio {
    public static final int PUNTOS_POR_NIVEL = 3;
    public static final int PV_POR_PUNTO = 12;
    public static final int PM_POR_PUNTO = 8;
    public static final int PV_AL_SUBIR = 30;
    public static final int PM_AL_SUBIR = 20;

    private Equilibrio() {
    }

    public static int experienciaSiguiente(int nivel) {
        return 75 + 35 * (nivel - 1);
    }

    public static int experienciaRonda(int ronda) {
        return 75 + 18 * (ronda - 1);
    }

    public static int oroRonda(int ronda, int turnos, int supervivientes, int consumibles) {
        return Math.max(60 + 13 * ronda,
                95 + 14 * ronda + 12 * supervivientes - 2 * turnos - 6 * consumibles);
    }
}
