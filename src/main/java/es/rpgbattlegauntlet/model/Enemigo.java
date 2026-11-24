package es.rpgbattlegauntlet.model;

import es.rpgbattlegauntlet.config.CatalogoObjetos;
import es.rpgbattlegauntlet.enums.TipoEnemigo;

import java.util.Objects;

public final class Enemigo extends Combatiente {

    private final TipoEnemigo tipo;
    private final double multiplicadorDanio;
    private Objeto pocionRevitalizacion;
    private int accionesRealizadas;
    private boolean ataqueUnicoIniciado;

    public Enemigo(TipoEnemigo tipo, EstadisticasBase estadisticasBase) {
        this(tipo, estadisticasBase, 1.0);
    }

    public Enemigo(TipoEnemigo tipo, EstadisticasBase estadisticasBase, double multiplicadorDanio) {
        super(estadisticasBase);
        this.tipo = Objects.requireNonNull(tipo);
        this.multiplicadorDanio = multiplicadorDanio;
        this.pocionRevitalizacion = tipo == TipoEnemigo.CABALLERO_OSCURO ? CatalogoObjetos.pocionRevitalizacion() : null;
    }

    public TipoEnemigo getTipo() {
        return tipo;
    }

    public double getMultiplicadorDanio() {
        return multiplicadorDanio;
    }

    public int getAccionesRealizadas() {
        return accionesRealizadas;
    }

    public boolean haIniciadoAtaqueUnico() {
        return ataqueUnicoIniciado;
    }

    void iniciarAtaqueUnico() {
        ataqueUnicoIniciado = true;
    }

    public boolean tienePocionRevitalizacion() {
        return pocionRevitalizacion != null;
    }

    Objeto retirarPocionRevitalizacion() {
        Objeto pocion = pocionRevitalizacion;
        pocionRevitalizacion = null;
        return pocion;
    }

    void registrarAccion() {
        accionesRealizadas++;
    }
}
