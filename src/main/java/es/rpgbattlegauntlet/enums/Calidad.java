package es.rpgbattlegauntlet.enums;

public enum Calidad {
    INFERIOR(0.75, 30, 60), NORMAL(1, 50, 100), SUPERIOR(1.25, 75, 150);

    private final double multiplicador;
    private final int venta;
    private final int recompra;

    Calidad(double multiplicador, int venta, int recompra) {
        this.multiplicador = multiplicador;
        this.venta = venta;
        this.recompra = recompra;
    }

    public int ajustar(int valor) {
        return (int) Math.round(Math.abs(valor) * multiplicador) * Integer.signum(valor);
    }

    public int venta(int base) {
        return (base * venta + 50) / 100;
    }

    public int recompra(int base) {
        return (base * recompra + 50) / 100;
    }
}
