package es.rpgbattlegauntlet.algorithm;

import es.rpgbattlegauntlet.model.Combatiente;
import es.rpgbattlegauntlet.model.Enemigo;
import es.rpgbattlegauntlet.model.Habilidad;
import es.rpgbattlegauntlet.model.Heroe;

import static es.rpgbattlegauntlet.enums.TipoEfecto.DANIO_FISICO;
import static es.rpgbattlegauntlet.enums.TipoEfecto.DANIO_MAGICO;

public final class CalculoCombate {

    private CalculoCombate() {
    }

    public static int danio(Combatiente atacante, Combatiente objetivo, Habilidad habilidad, boolean critico) {
        return danio(atacante, objetivo, habilidad, critico, 0);
    }

    public static int danio(Combatiente atacante, Combatiente objetivo, Habilidad habilidad, boolean critico, int reduccionMinima) {
        var origen = atacante.getEstadisticasBase();
        var bonos = atacante.getBonificaciones();
        boolean magico = habilidad.efecto() == DANIO_MAGICO;
        double potencia = magico ? origen.inteligencia() + bonos.inteligencia() + bonos.potenciaMagica()
                : 2.0 * (origen.fuerza() + bonos.fuerza());
        double defensa = magico ? objetivo.getResistenciaEfectiva() : objetivo.getDefensaEfectiva();
        double mitigacion = defensa * (100 - habilidad.penetracion()) / 100.0;
        double bruto = potencia + habilidad.potencia();
        if (magico && atacante instanceof Heroe) {
            bruto = Math.min(64, bruto);
        }
        double multiplicador = atacante instanceof Enemigo enemigo ? enemigo.getMultiplicadorDanio() : 1.0;
        if (habilidad.efecto() == DANIO_FISICO) {
            double fraccionVidaPerdida = 1 - (double) atacante.getPvActuales() / origen.pvMaximos();
            multiplicador *= 1 + fraccionVidaPerdida * atacante.getBonoMaximoFuriaMaldita() / 100;
        }
        int reduccion = Math.max(objetivo.getReduccionDanio(), reduccionMinima);
        return aplicarMitigacion(bruto, mitigacion, critico, multiplicador, reduccion);
    }

    public static int danioPergamino(Combatiente objetivo, int potencia) {
        return aplicarMitigacion(potencia, objetivo.getResistenciaEfectiva(), false, 1.0, objetivo.getReduccionDanio());
    }

    private static int aplicarMitigacion(double bruto, double mitigacion, boolean critico, double multiplicador, int reduccion) {
        double reducido = Math.max(bruto * 0.25, bruto - mitigacion);
        double multiplicadorCritico = critico ? 1.5 : 1.0;
        double finalTrasProteccion = reducido * multiplicadorCritico * (100 - reduccion) / 100.0;
        return Math.max(1, (int) Math.round(finalTrasProteccion * multiplicador));
    }

    public static int curacion(Combatiente atacante, Habilidad habilidad) {
        var bonos = atacante.getBonificaciones();
        return Math.min(89, habilidad.potencia() + 2 * (atacante.getEstadisticasBase().inteligencia()
                + bonos.inteligencia()) + bonos.potenciaMagica());
    }
}
