package es.rpgbattlegauntlet.algorithm;

import es.rpgbattlegauntlet.model.Combatiente;
import es.rpgbattlegauntlet.model.Enemigo;
import es.rpgbattlegauntlet.model.Habilidad;
import es.rpgbattlegauntlet.model.Heroe;

import java.util.Comparator;
import java.util.List;

import static es.rpgbattlegauntlet.config.CatalogoEnemigos.*;
import static es.rpgbattlegauntlet.config.CatalogoHabilidades.ATAQUE_BASICO;

public final class InteligenciaEnemiga {

    public record Decision(Habilidad habilidad, Combatiente objetivo) {
    }

    private InteligenciaEnemiga() {
    }

    public static Decision decidir(Enemigo enemigo, List<Combatiente> aliados, List<Heroe> heroes) {
        Heroe vulnerable = heroes.stream().filter(Combatiente::estaVivo)
                .min(Comparator.comparingInt(Combatiente::getPvActuales)).orElseThrow();
        int accion = enemigo.getAccionesRealizadas();
        int vida = porcentajeVida(enemigo);
        return switch (enemigo.getTipo()) {
            case GOBLIN -> new Decision(accion % 3 == 1 ? ATAQUE_DOBLE : CUCHILLADA, vulnerable);
            case ESQUELETO -> accion % 3 == 1 || (vida <= 35 && accion % 3 == 0)
                    ? new Decision(GUARDIA_OSEA, enemigo) : new Decision(ESPADAZO, vulnerable);
            case CHAMAN -> decisionChaman(enemigo, aliados, vulnerable);
            case OGRO -> accion % 2 == 1 && heroes.stream().filter(Combatiente::estaVivo).count() > 1
                    ? new Decision(BARRIDO, null) : new Decision(MAZAZO, vulnerable);
            case CABALLERO_OSCURO -> {
                if (accion % 4 == 1 && puedeUsar(enemigo, GUARDIA_OSCURA)) {
                    yield new Decision(GUARDIA_OSCURA, enemigo);
                }
                if (accion % 4 == 2 && puedeUsar(enemigo, PREPARAR_CONTRAATAQUE)) {
                    yield new Decision(PREPARAR_CONTRAATAQUE, enemigo);
                }
                yield new Decision(ESPADA_MALDITA, vulnerable);
            }
            case DRAGON -> decisionDragon(enemigo, vulnerable, accion, vida);
        };
    }

    private static Decision decisionChaman(Enemigo chaman, List<Combatiente> aliados, Heroe vulnerable) {
        if (puedeUsar(chaman, CURACION_OSCURA)) {
            var herido = aliados.stream().filter(Combatiente::estaVivo)
                    .filter(aliado -> porcentajeVida(aliado) <= 70)
                    .min(Comparator.comparingDouble(aliado ->
                            (double) aliado.getPvActuales() / aliado.getEstadisticasBase().pvMaximos()));
            if (herido.isPresent()) {
                return new Decision(CURACION_OSCURA, herido.get());
            }
        }
        return new Decision(puedeUsar(chaman, BOLA_FUEGO) ? BOLA_FUEGO : ATAQUE_BASICO, vulnerable);
    }

    private static Decision decisionDragon(Enemigo dragon, Heroe vulnerable, int accion, int vida) {
        if (vida <= 35 && accion % 3 == 1 && puedeUsar(dragon, RUGIDO)) {
            return new Decision(RUGIDO, null);
        }
        boolean usarAliento = vida <= 35 ? accion % 3 != 1 : vida <= 70 && accion % 2 == 1;
        if (usarAliento && puedeUsar(dragon, ALIENTO)) {
            return new Decision(ALIENTO, null);
        }
        return new Decision(GARRA, vulnerable);
    }

    private static boolean puedeUsar(Enemigo enemigo, Habilidad habilidad) {
        return enemigo.getPmActuales() >= habilidad.costePm();
    }

    private static int porcentajeVida(Combatiente combatiente) {
        int maximos = combatiente.getEstadisticasBase().pvMaximos();
        return (int) Math.ceilDiv(100L * combatiente.getPvActuales(), maximos);
    }
}
