package es.rpgbattlegauntlet.algorithm;

import es.rpgbattlegauntlet.model.Combatiente;
import es.rpgbattlegauntlet.model.Enemigo;
import es.rpgbattlegauntlet.model.Habilidad;
import es.rpgbattlegauntlet.model.Heroe;
import es.rpgbattlegauntlet.enums.TipoObjetivo;

import java.util.Comparator;
import java.util.List;
import java.util.random.RandomGenerator;

import static es.rpgbattlegauntlet.config.CatalogoEnemigos.*;
import static es.rpgbattlegauntlet.config.CatalogoHabilidades.ATAQUE_BASICO;

public final class InteligenciaEnemiga {

    public record Decision(Habilidad habilidad, Combatiente objetivo) {
    }

    private InteligenciaEnemiga() {
    }

    public static Combatiente aliadoParaRevitalizar(Enemigo enemigo, List<Combatiente> aliados) {
        if (!enemigo.tienePocionRevitalizacion()) return null;
        return aliados.stream().filter(aliado -> !aliado.estaVivo()).findFirst().orElse(null);
    }

    public static Decision decidir(Enemigo enemigo, List<Combatiente> aliados, List<Heroe> heroes, RandomGenerator azar) {
        int accion = enemigo.getAccionesRealizadas();
        int vida = porcentajeVida(enemigo);
        Decision decision = switch (enemigo.getTipo()) {
            case GOBLIN -> new Decision(accion % 3 == 1 ? ATAQUE_DOBLE : CUCHILLADA, null);
            case ESQUELETO -> accion % 3 == 1 || (vida <= 35 && accion % 3 == 0)
                    ? new Decision(GUARDIA_OSEA, enemigo) : new Decision(ESPADAZO, null);
            case CHAMAN -> decisionChaman(enemigo, aliados);
            case OGRO -> accion % 2 == 1 && heroes.stream().filter(Combatiente::estaVivo).count() > 1
                    ? new Decision(BARRIDO, null) : new Decision(MAZAZO, null);
            case CABALLERO_OSCURO -> {
                if (accion % 4 == 1 && puedeUsar(enemigo, GUARDIA_OSCURA)) {
                    yield new Decision(GUARDIA_OSCURA, enemigo);
                }
                if (accion % 4 == 2 && puedeUsar(enemigo, PREPARAR_CONTRAATAQUE)) {
                    yield new Decision(PREPARAR_CONTRAATAQUE, enemigo);
                }
                yield new Decision(ESPADA_MALDITA, null);
            }
            case DRAGON -> decisionDragon(enemigo, accion, vida);
        };
        return decision.habilidad().objetivo() == TipoObjetivo.ENEMIGO
                ? new Decision(decision.habilidad(), elegirObjetivo(heroes, azar)) : decision;
    }

    private static Heroe elegirObjetivo(List<Heroe> heroes, RandomGenerator azar) {
        var vivos = heroes.stream().filter(Combatiente::estaVivo).toList();
        if (vivos.size() == 1) return vivos.getFirst();

        var candidatos = vivos;
        if (azar.nextInt(100) < 25) {
            var porProporcion = Comparator.comparingDouble(InteligenciaEnemiga::proporcionVida);
            Heroe masHerido = vivos.stream().min(porProporcion).orElseThrow();
            candidatos = vivos.stream().filter(heroe -> porProporcion.compare(heroe, masHerido) == 0).toList();
        }
        return candidatos.size() == 1 ? candidatos.getFirst() : candidatos.get(azar.nextInt(candidatos.size()));
    }

    private static double proporcionVida(Combatiente combatiente) {
        return (double) combatiente.getPvActuales() / combatiente.getEstadisticasBase().pvMaximos();
    }

    private static Decision decisionChaman(Enemigo chaman, List<Combatiente> aliados) {
        if (puedeUsar(chaman, CURACION_OSCURA)) {
            var herido = aliados.stream().filter(Combatiente::estaVivo)
                    .filter(aliado -> porcentajeVida(aliado) <= 70)
                    .min(Comparator.comparingDouble(InteligenciaEnemiga::proporcionVida));
            if (herido.isPresent()) {
                return new Decision(CURACION_OSCURA, herido.get());
            }
        }
        return new Decision(puedeUsar(chaman, BOLA_FUEGO) ? BOLA_FUEGO : ATAQUE_BASICO, null);
    }

    private static Decision decisionDragon(Enemigo dragon, int accion, int vida) {
        if (vida <= 35 && accion % 3 == 1 && puedeUsar(dragon, RUGIDO)) {
            return new Decision(RUGIDO, null);
        }
        boolean usarAliento = vida <= 35 ? accion % 3 != 1 : vida <= 70 && accion % 2 == 1;
        if (usarAliento && puedeUsar(dragon, ALIENTO)) {
            return new Decision(ALIENTO, null);
        }
        return new Decision(GARRA, null);
    }

    private static boolean puedeUsar(Enemigo enemigo, Habilidad habilidad) {
        return enemigo.getPmActuales() >= habilidad.costePm();
    }

    private static int porcentajeVida(Combatiente combatiente) {
        int maximos = combatiente.getEstadisticasBase().pvMaximos();
        return (int) Math.ceilDiv(100L * combatiente.getPvActuales(), maximos);
    }
}
