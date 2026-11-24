package es.rpgbattlegauntlet.algorithm;

import es.rpgbattlegauntlet.model.Combatiente;
import es.rpgbattlegauntlet.model.Enemigo;
import es.rpgbattlegauntlet.model.Habilidad;
import es.rpgbattlegauntlet.model.Heroe;

import static es.rpgbattlegauntlet.enums.TipoEfecto.DANIO_MAGICO;

public final class CalculoCombate {

    private CalculoCombate() {
    }

    public static int danio(Combatiente atacante, Combatiente objetivo, Habilidad habilidad, boolean critico) {
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
        return aplicarMitigacion(objetivo, bruto, mitigacion, critico, multiplicador);
    }

    public static int danioPergamino(Combatiente objetivo, int potencia) {
        return aplicarMitigacion(objetivo, potencia, objetivo.getResistenciaEfectiva(), false, 1.0);
    }

    private static int aplicarMitigacion(Combatiente objetivo, double bruto, double mitigacion, boolean critico,
                                        double multiplicador) {
        double reducido = Math.max(bruto * 0.25, bruto - mitigacion);
        double multiplicadorCritico = critico ? 1.5 : 1.0;
        double finalTrasProteccion = reducido * multiplicadorCritico * (100 - objetivo.getReduccionDanio()) / 100.0;
        return Math.max(1, (int) Math.round(finalTrasProteccion * multiplicador));
    }

    public static int curacion(Combatiente atacante, Habilidad habilidad) {
        var bonos = atacante.getBonificaciones();
        return Math.min(89, habilidad.potencia() + 2 * (atacante.getEstadisticasBase().inteligencia()
                + bonos.inteligencia()) + bonos.potenciaMagica());
    }
}
