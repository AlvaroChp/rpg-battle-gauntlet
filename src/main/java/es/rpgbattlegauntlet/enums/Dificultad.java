package es.rpgbattlegauntlet.enums;

public enum Dificultad {
    FACIL(100, 100), DIFICIL(125, 150), EXPERTO(130, 200), PRUEBAS(0, 100);

    private final int porcentajeVida;
    private final int porcentajeDanio;

    Dificultad(int porcentajeVida, int porcentajeDanio) {
        this.porcentajeVida = porcentajeVida;
        this.porcentajeDanio = porcentajeDanio;
    }

    public int vidaEnemiga(int vidaBase) {
        return Math.max(1, (int) Math.round(vidaBase * porcentajeVida / 100.0));
    }

    public double multiplicadorDanioEnemigo() {
        return porcentajeDanio / 100.0;
    }
}
