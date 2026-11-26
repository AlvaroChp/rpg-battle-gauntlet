package es.rpgbattlegauntlet.model;

import es.rpgbattlegauntlet.config.Equilibrio;

public record Recompensa(int ronda, int oro, int experiencia, int turnos, int supervivientes, int consumibles) {
    public Equilibrio.DesgloseOro getDesgloseOro() {
        return Equilibrio.desgloseOroRonda(ronda, turnos, supervivientes, consumibles);
    }
}
