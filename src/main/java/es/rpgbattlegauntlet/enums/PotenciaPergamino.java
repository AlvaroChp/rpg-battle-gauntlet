package es.rpgbattlegauntlet.enums;

public enum PotenciaPergamino {
    DEBIL(-2, 30), NORMAL(0, 50), PODEROSO(3, 80);

    private final int ajuste;
    private final int venta;

    PotenciaPergamino(int ajuste, int venta) {
        this.ajuste = ajuste;
        this.venta = venta;
    }

    public int requisito(int base) {
        return base + ajuste;
    }

    public int venta(int base) {
        return (base * venta + 50) / 100;
    }
}
