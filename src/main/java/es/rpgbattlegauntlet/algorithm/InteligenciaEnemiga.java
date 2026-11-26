package es.rpgbattlegauntlet.algorithm;

import es.rpgbattlegauntlet.model.Combatiente;
import es.rpgbattlegauntlet.model.Enemigo;
import es.rpgbattlegauntlet.model.Habilidad;
import es.rpgbattlegauntlet.model.Heroe;
import es.rpgbattlegauntlet.model.EstadisticasBase;
import es.rpgbattlegauntlet.config.CatalogoObjetos;
import es.rpgbattlegauntlet.enums.TipoObjetivo;
import es.rpgbattlegauntlet.enums.TipoEnemigo;
import es.rpgbattlegauntlet.enums.EstadoTemporal;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.random.RandomGenerator;

import static es.rpgbattlegauntlet.config.CatalogoEnemigos.*;
import static es.rpgbattlegauntlet.config.CatalogoHabilidades.ATAQUE_BASICO;
import static es.rpgbattlegauntlet.enums.TipoEfecto.DANIO_FISICO;
import static es.rpgbattlegauntlet.enums.TipoEfecto.DANIO_MAGICO;

public final class InteligenciaEnemiga {

    public record Decision(Habilidad habilidad, Combatiente objetivo) {
    }

    private InteligenciaEnemiga() {
    }

    private static final Map<Habilidad, Habilidad> PREPARACIONES = Map.of(
            FURIA_ANIMAL, PREPARAR_FURIA_ANIMAL,
            CONTRAATAQUE, PREPARAR_CONTRAATAQUE,
            ONDA_RUNICA, PREPARAR_ONDA_RUNICA,
            SENTENCIA, MARCAR_SENTENCIA,
            INCENDIO_CENIZA, PREPARAR_INCENDIO_CENIZA,
            NUBE, PREPARAR_NUBE,
            ALIENTO_GLACIAL, PREPARAR_ALIENTO_GLACIAL,
            DERRUMBE, PREPARAR_DERRUMBE,
            RUPTURA, PREPARAR_RUPTURA);

    public static List<Habilidad> habilidadesBestiario(Enemigo enemigo) {
        var propias = habilidadesPropias(enemigo.getTipo());
        var especiales = new LinkedHashMap<String, Habilidad>();
        Stream.concat(propias.stream().skip(1), GeneradorBotin.habilidadesDeArmas(enemigo.getTipo()).stream())
                .filter(habilidad -> !PREPARACIONES.containsValue(habilidad))
                .forEach(habilidad -> especiales.putIfAbsent(habilidad.id(), habilidad));
        var objetivoSinDefensa = new Combatiente(new EstadisticasBase(1, 0, 0, 0, 0, 0, 1, 0, 0));
        var ordenadas = especiales.values().stream()
                .sorted(Comparator.comparingInt(habilidad -> habilidad.efecto() == DANIO_FISICO || habilidad.efecto() == DANIO_MAGICO
                        ? CalculoCombate.danio(enemigo, objetivoSinDefensa, habilidad, false) * habilidad.golpes()
                        : Integer.MAX_VALUE))
                .flatMap(habilidad -> PREPARACIONES.containsKey(habilidad)
                        ? Stream.of(PREPARACIONES.get(habilidad), habilidad) : Stream.of(habilidad));
        return Stream.concat(Stream.of(propias.getFirst()), ordenadas).toList();
    }

    private static List<Habilidad> habilidadesPropias(TipoEnemigo tipo) {
        return switch (tipo) {
            case GOBLIN -> List.of(CUCHILLADA, ATAQUE_DOBLE);
            case GOBLIN_ARQUERO -> List.of(FLECHAZO_GOBLIN);
            case ESQUELETO -> List.of(ESPADAZO, GUARDIA_OSEA);
            case CHAMAN -> List.of(ataqueDeReserva(tipo), BOLA_FUEGO, CURACION_OSCURA);
            case OGRO -> List.of(MAZAZO, BARRIDO, PREPARAR_FURIA_ANIMAL, FURIA_ANIMAL);
            case CABALLERO_OSCURO -> List.of(ESPADA_MALDITA, GUARDIA_OSCURA, PREPARAR_CONTRAATAQUE, CONTRAATAQUE);
            case NIGROMANTE -> List.of(ataqueDeReserva(tipo), DESCARGA_FUNEBRE, REANIMAR);
            case GOLEM_RUNICO -> List.of(ataqueDeReserva(tipo), PUNO_DE_PIEDRA, PREPARAR_ONDA_RUNICA, ONDA_RUNICA);
            case VERDUGO_ESPECTRAL -> List.of(ataqueDeReserva(tipo), TAJO_ESPECTRAL, MARCAR_SENTENCIA, SENTENCIA, BARRIDO_ESPECTRAL);
            case DRAGON -> List.of(GARRA, ALIENTO, RUGIDO, PREPARAR_INCENDIO_CENIZA, INCENDIO_CENIZA);
            case DRACONIANO_CIMITARRA -> List.of(TAJO_DRACONICO, CORTE_ESCAMAS);
            case DRACONIANO_BALLESTA -> List.of(VIROTE_DRACONICO, virotePerforante());
            case GUIVERNO -> List.of(AGUIJONAZO, PICADO, PREPARAR_NUBE, NUBE);
            case DRAGON_ESCARCHA -> List.of(GARRA_HELADA, LANZA_HIELO, PREPARAR_ALIENTO_GLACIAL, ALIENTO_GLACIAL);
            case DRAGON_OBSIDIANA -> List.of(COLETAZO, ALIENTO_MAGMA, PREPARAR_DERRUMBE, DERRUMBE);
            case DIOS_DRAGON -> List.of(GARRA_ANCESTRAL, ALIENTO_ANCESTRAL, PREPARAR_RUPTURA, RUPTURA);
        };
    }

    private static Habilidad ataqueDeReserva(TipoEnemigo tipo) {
        return ATAQUE_BASICO.conNombre(switch (tipo) {
            case CHAMAN, NIGROMANTE -> "Golpe de vara";
            case GOLEM_RUNICO -> "Golpe de piedra";
            case VERDUGO_ESPECTRAL -> "Hachazo";
            default -> throw new IllegalArgumentException("El enemigo ya dispone de un ataque básico propio");
        });
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
            if (puedeUsar(enemigo, INCENDIO_CENIZA)) {
                if (enemigo.tieneEstado(EstadoTemporal.CARGA_INCENDIO_CENIZA)) return new Decision(INCENDIO_CENIZA, null);
                if (vida <= 35 && !enemigo.haIniciadoAtaqueUnico()) return new Decision(PREPARAR_INCENDIO_CENIZA, enemigo);
            }
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
            case DRACONIANO_CIMITARRA -> new Decision(alternar(enemigo, TAJO_DRACONICO, CORTE_ESCAMAS), null);
            case DRACONIANO_BALLESTA -> new Decision(alternar(enemigo, VIROTE_DRACONICO, virotePerforante()), null);
            case GUIVERNO -> ataqueAnunciado(enemigo, EstadoTemporal.CARGA_NUBE_PONZONOSA, PREPARAR_NUBE, NUBE,
                    alternar(enemigo, AGUIJONAZO, PICADO));
            case DRAGON_ESCARCHA -> ataqueAnunciado(enemigo, EstadoTemporal.CARGA_ALIENTO_GLACIAL, PREPARAR_ALIENTO_GLACIAL,
                    ALIENTO_GLACIAL, alternar(enemigo, GARRA_HELADA, LANZA_HIELO));
            case DRAGON_OBSIDIANA -> ataqueAnunciado(enemigo, EstadoTemporal.CARGA_DERRUMBE, PREPARAR_DERRUMBE,
                    DERRUMBE, alternar(enemigo, COLETAZO, ALIENTO_MAGMA));
            case DIOS_DRAGON -> decisionDiosDragon(enemigo, vida);
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
        if (turnoDeArma && ataque != ATAQUE_DOBLE && !enemigo.getTipo().perteneceAlReinoDraconico()) {
            var disponibles = GeneradorBotin.habilidadesDeArmas(enemigo.getTipo()).stream()
                    .filter(habilidad -> puedeUsar(enemigo, habilidad)).toList();
            if (!disponibles.isEmpty()) {
                ataque = disponibles.size() == 1 ? disponibles.getFirst() : disponibles.get(azar.nextInt(disponibles.size()));
            }
        }
        return new Decision(ataque, ataque.objetivo() == TipoObjetivo.ENEMIGO ? elegirObjetivo(heroes, azar) : null);
    }

    private static Habilidad virotePerforante() {
        return CatalogoObjetos.habilidadArma("ballesta").orElseThrow();
    }

    private static Habilidad alternar(Enemigo enemigo, Habilidad basico, Habilidad especial) {
        return enemigo.getAccionesRealizadas() % 2 == 1 && puedeUsar(enemigo, especial) ? especial : basico;
    }

    private static Decision ataqueAnunciado(Enemigo enemigo, EstadoTemporal carga, Habilidad preparar,
                                            Habilidad ataque, Habilidad alternativa) {
        if (enemigo.tieneEstado(carga)) {
            return new Decision(puedeUsar(enemigo, ataque) ? ataque : alternativa, null);
        }
        return enemigo.getAccionesRealizadas() % 4 == 2 && puedeUsar(enemigo, ataque)
                ? new Decision(preparar, enemigo) : new Decision(alternativa, null);
    }

    private static Decision decisionDiosDragon(Enemigo enemigo, int vida) {
        if (enemigo.tieneEstado(EstadoTemporal.CARGA_RUPTURA_VINCULO)) return new Decision(RUPTURA, null);
        if (vida <= 35 && !enemigo.haIniciadoAtaqueUnico()) return new Decision(PREPARAR_RUPTURA, enemigo);
        return new Decision(alternar(enemigo, GARRA_ANCESTRAL, ALIENTO_ANCESTRAL), null);
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
        return new Decision(puedeUsar(chaman, BOLA_FUEGO) ? BOLA_FUEGO : ataqueDeReserva(chaman.getTipo()), null);
    }

    private static Decision decisionNigromante(Enemigo enemigo, List<Combatiente> aliados) {
        if (!enemigo.haUsadoReanimacion() && puedeUsar(enemigo, REANIMAR)) {
            var caido = aliados.stream().filter(aliado -> !aliado.estaVivo()).findFirst();
            if (caido.isPresent()) return new Decision(REANIMAR, caido.get());
        }
        return new Decision(puedeUsar(enemigo, DESCARGA_FUNEBRE) ? DESCARGA_FUNEBRE : ataqueDeReserva(enemigo.getTipo()), null);
    }

    private static Decision decisionGolem(Enemigo enemigo, int accion) {
        if (enemigo.tieneEstado(EstadoTemporal.CARGA_ONDA_RUNICA) && puedeUsar(enemigo, ONDA_RUNICA)) {
            return new Decision(ONDA_RUNICA, null);
        }
        if (accion % 3 == 0 && enemigo.getPmActuales() >= enemigo.costeHabilidad(PREPARAR_ONDA_RUNICA) + ONDA_RUNICA.costePm()) {
            return new Decision(PREPARAR_ONDA_RUNICA, enemigo);
        }
        return new Decision(puedeUsar(enemigo, PUNO_DE_PIEDRA) ? PUNO_DE_PIEDRA : ataqueDeReserva(enemigo.getTipo()), null);
    }

    private static Decision decisionVerdugo(Enemigo enemigo, List<Heroe> heroes, int accion) {
        if (enemigo.getObjetivoMarcado() != null && puedeUsar(enemigo, SENTENCIA)) {
            var marcado = heroes.stream().filter(Heroe::estaVivo)
                    .filter(heroe -> heroe.getId().equals(enemigo.getObjetivoMarcado())).findFirst();
            if (marcado.isPresent()) return new Decision(SENTENCIA, marcado.get());
        }
        if (accion % 4 == 1 && enemigo.getPmActuales() >= enemigo.costeHabilidad(MARCAR_SENTENCIA) + SENTENCIA.costePm()) {
            return new Decision(MARCAR_SENTENCIA, null);
        }
        if (accion % 4 == 3 && puedeUsar(enemigo, BARRIDO_ESPECTRAL)) {
            return new Decision(BARRIDO_ESPECTRAL, null);
        }
        return new Decision(puedeUsar(enemigo, TAJO_ESPECTRAL) ? TAJO_ESPECTRAL : ataqueDeReserva(enemigo.getTipo()), null);
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
        return enemigo.getPmActuales() >= enemigo.costeHabilidad(habilidad);
    }

    private static int porcentajeVida(Combatiente combatiente) {
        int maximos = combatiente.getEstadisticasBase().pvMaximos();
        return (int) Math.ceilDiv(100L * combatiente.getPvActuales(), maximos);
    }
}
