package es.rpgbattlegauntlet.algorithm;

import es.rpgbattlegauntlet.model.Combatiente;
import es.rpgbattlegauntlet.model.Enemigo;
import es.rpgbattlegauntlet.model.Habilidad;
import es.rpgbattlegauntlet.model.Heroe;
import es.rpgbattlegauntlet.enums.TipoObjetivo;
import es.rpgbattlegauntlet.enums.TipoEnemigo;
import es.rpgbattlegauntlet.enums.EstadoTemporal;

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
        if (enemigo.getTipo() == TipoEnemigo.OGRO) {
            if (enemigo.tieneEstado(EstadoTemporal.CARGA_FURIA_ANIMAL)) return new Decision(FURIA_ANIMAL, null);
            if (vida <= 50 && !enemigo.haIniciadoAtaqueUnico()) return new Decision(PREPARAR_FURIA_ANIMAL, enemigo);
        }
        if (enemigo.getTipo() == TipoEnemigo.DRAGON) {
            if (enemigo.tieneEstado(EstadoTemporal.CARGA_INCENDIO_CENIZA)) return new Decision(INCENDIO_CENIZA, null);
            if (vida <= 35 && !enemigo.haIniciadoAtaqueUnico()) return new Decision(PREPARAR_INCENDIO_CENIZA, enemigo);
        }
        Decision decision = switch (enemigo.getTipo()) {
            case GOBLIN -> new Decision(accion % 3 == 1 && puedeUsar(enemigo, ATAQUE_DOBLE) ? ATAQUE_DOBLE : CUCHILLADA, null);
            case GOBLIN_ARQUERO -> new Decision(FLECHAZO_GOBLIN, null);
            case ESQUELETO -> (accion % 3 == 1 || (vida <= 35 && accion % 3 == 0)) && puedeUsar(enemigo, GUARDIA_OSEA)
                    ? new Decision(GUARDIA_OSEA, enemigo) : new Decision(ESPADAZO, null);
            case CHAMAN -> decisionChaman(enemigo, aliados);
            case OGRO -> accion % 2 == 1 && puedeUsar(enemigo, BARRIDO) && heroes.stream().filter(Combatiente::estaVivo).count() > 1
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
            case NIGROMANTE -> decisionNigromante(enemigo, aliados);
            case GOLEM_RUNICO -> decisionGolem(enemigo, accion);
            case VERDUGO_ESPECTRAL -> decisionVerdugo(enemigo, heroes, accion);
            case DRAGON -> decisionDragon(enemigo, accion, vida);
        };
        if (decision.habilidad().objetivo() != TipoObjetivo.ENEMIGO || decision.objetivo() != null) {
            return decision;
        }

        Habilidad ataque = decision.habilidad();
        boolean turnoDeArma = switch (enemigo.getTipo()) {
            case CABALLERO_OSCURO -> accion % 4 == 3;
            case GOBLIN_ARQUERO -> accion % 2 == 1;
            default -> accion % 3 == 2;
        };
        if (turnoDeArma && ataque != ATAQUE_DOBLE) {
            var disponibles = GeneradorBotin.habilidadesDeArmas(enemigo.getTipo()).stream()
                    .filter(habilidad -> puedeUsar(enemigo, habilidad)).toList();
            if (!disponibles.isEmpty()) {
                ataque = disponibles.size() == 1 ? disponibles.getFirst() : disponibles.get(azar.nextInt(disponibles.size()));
            }
        }
        return new Decision(ataque, ataque.objetivo() == TipoObjetivo.ENEMIGO ? elegirObjetivo(heroes, azar) : null);
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

    private static Decision decisionNigromante(Enemigo enemigo, List<Combatiente> aliados) {
        if (!enemigo.haUsadoReanimacion() && puedeUsar(enemigo, REANIMAR)) {
            var caido = aliados.stream().filter(aliado -> !aliado.estaVivo()).findFirst();
            if (caido.isPresent()) return new Decision(REANIMAR, caido.get());
        }
        return new Decision(puedeUsar(enemigo, DESCARGA_FUNEBRE) ? DESCARGA_FUNEBRE : ATAQUE_BASICO, null);
    }

    private static Decision decisionGolem(Enemigo enemigo, int accion) {
        if (enemigo.tieneEstado(EstadoTemporal.CARGA_ONDA_RUNICA)) return new Decision(ONDA_RUNICA, null);
        return accion % 3 == 0 ? new Decision(PREPARAR_ONDA_RUNICA, enemigo) : new Decision(PUNO_DE_PIEDRA, null);
    }

    private static Decision decisionVerdugo(Enemigo enemigo, List<Heroe> heroes, int accion) {
        if (enemigo.getObjetivoMarcado() != null) {
            var marcado = heroes.stream().filter(Heroe::estaVivo)
                    .filter(heroe -> heroe.getId().equals(enemigo.getObjetivoMarcado())).findFirst();
            if (marcado.isPresent()) return new Decision(SENTENCIA, marcado.get());
        }
        return switch (accion % 4) {
            case 1 -> new Decision(MARCAR_SENTENCIA, null);
            case 3 -> new Decision(BARRIDO_ESPECTRAL, null);
            default -> new Decision(TAJO_ESPECTRAL, null);
        };
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
