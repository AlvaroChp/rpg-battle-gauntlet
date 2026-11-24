package es.rpgbattlegauntlet.model;

import es.rpgbattlegauntlet.config.CatalogoObjetos;
import es.rpgbattlegauntlet.config.Equilibrio;
import es.rpgbattlegauntlet.enums.TipoEnemigo;

import java.util.Objects;
import java.util.UUID;

public final class Enemigo extends Combatiente {

    private final TipoEnemigo tipo;
    private final double multiplicadorDanio;
    private Objeto pocionRevitalizacion;
    private int accionesRealizadas;
    private boolean ataqueUnicoIniciado;
    private boolean reanimacionUsada;
    private UUID objetivoMarcado;

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

    @Override
    public int getBonoMaximoFuriaMaldita() {
        return tipo == TipoEnemigo.CABALLERO_OSCURO ? Equilibrio.BONO_MAXIMO_FURIA_MALDITA : 0;
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

    public boolean haUsadoReanimacion() {
        return reanimacionUsada;
    }

    void registrarReanimacion() {
        reanimacionUsada = true;
    }

    public UUID getObjetivoMarcado() {
        return objetivoMarcado;
    }

    void marcarObjetivo(UUID objetivo) {
        objetivoMarcado = objetivo;
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
