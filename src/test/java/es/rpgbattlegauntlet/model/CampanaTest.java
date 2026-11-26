package es.rpgbattlegauntlet.model;

import es.rpgbattlegauntlet.algorithm.CalculoCombate;
import es.rpgbattlegauntlet.enums.Atributo;
import es.rpgbattlegauntlet.enums.EstadoPartida;
import es.rpgbattlegauntlet.enums.TipoConsumible;
import es.rpgbattlegauntlet.enums.TipoEnemigo;
import es.rpgbattlegauntlet.enums.TipoObjetivo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.stream.IntStream;

import static es.rpgbattlegauntlet.enums.Atributo.*;
import static es.rpgbattlegauntlet.enums.ClaseHeroe.*;
import static org.junit.jupiter.api.Assertions.*;

class CampanaTest {

    static IntStream semillas() {
        return IntStream.range(0, 30);
    }

    @ParameterizedTest
    @MethodSource("semillas")
    void puedeDerrotarAlDragonSinRecogerBotin(int semilla) {
        var partida = new Partida(new Random(semilla));
        partida.seleccionarHeroes(List.of(ARQUERO, SANADOR, GUERRERO));
        partida.iniciarCombate();
        for (int ronda = 1; ronda <= 10; ronda++) {
            int oroAntes = partida.getOro();
            var nivelesAntes = partida.getEquipo().stream().map(Heroe::getNivel).toList();
            var experienciaAntes = partida.getEquipo().stream().map(Heroe::getExperiencia).toList();
            resolverCombate(partida);
            assertEquals(ronda, partida.getRonda());
            if (ronda < 10) {
                assertEquals(EstadoPartida.ENTRE_COMBATES, partida.getEstado());
                prepararEquipo(partida);
                partida.avanzarRonda(ronda);
            } else {
                assertEquals(EstadoPartida.VICTORIA, partida.getEstado());
                assertEquals(oroAntes, partida.getOro());
                assertEquals(nivelesAntes, partida.getEquipo().stream().map(Heroe::getNivel).toList());
                assertEquals(experienciaAntes, partida.getEquipo().stream().map(Heroe::getExperiencia).toList());
                assertTrue(partida.getBotin().isEmpty());
                assertNull(partida.getRecompensa());
            }
        }
    }

    private static void resolverCombate(Partida partida) {
        var combate = partida.getCombate();
        for (int acciones = 0; acciones < 150 && partida.getEstado() == EstadoPartida.COMBATE; acciones++) {
            var actor = (Heroe) combate.getOrdenActuacion().getFirst();
            var vivos = combate.getEnemigos().stream().filter(Combatiente::estaVivo).toList();
            var objetivo = vivos.stream().min(Comparator.comparingInt(CampanaTest::prioridadEnemigo)).orElseThrow();
            var vulnerable = partida.getEquipo().stream().filter(Heroe::estaVivo)
                    .min(Comparator.comparingInt(Heroe::getPvActuales)).orElseThrow();
            var habilidades = combate.getHabilidadesDisponibles();
            int ronda = partida.getRonda();
            int falta = vidaFaltante(vulnerable);
            if (ronda == 10 && vulnerable.getPvActuales() < 100 && vulnerable.getReduccionDanio() == 0
                    && usarSiExiste(partida, actor, buscar(partida, "proteccion").filter(objeto -> objeto.cumpleRequisitos(actor)), null)) {
                continue;
            }
            if (falta >= 45 && habilidades.stream().anyMatch(habilidad -> habilidad.id().equals("curacion"))) {
                actuar(partida, actor, "curacion", vulnerable);
                continue;
            }
            if (falta >= 55 && vulnerable.getPvActuales() < (ronda == 10 ? 90 : 42)
                    && usarSiExiste(partida, actor, buscar(partida, "roja"), vulnerable)) {
                continue;
            }
            if (actor.getClase() == SANADOR && actor.getPmActuales() < 13
                    && usarSiExiste(partida, actor, buscar(partida, "verde"), actor)) {
                continue;
            }
            if (ronda >= 9 && (vivos.size() > 1 || objetivo.getPvActuales() > 90)) {
                var ofensivo = partida.getInventario().stream().filter(objeto -> objeto.efecto() != null)
                        .filter(objeto -> objeto.efecto().tipo() == TipoConsumible.DANIO && objeto.cumpleRequisitos(actor))
                        .max(Comparator.comparingInt(objeto -> objeto.efecto().cantidad()));
                if (ofensivo.isPresent()) {
                    usarSiExiste(partida, actor, ofensivo,
                            ofensivo.get().efecto().objetivo() == TipoObjetivo.ENEMIGOS ? null : objetivo);
                    continue;
                }
            }
            var ataque = habilidades.stream().filter(habilidad -> habilidad.objetivo() == TipoObjetivo.ENEMIGO)
                    .filter(habilidad -> ronda >= 7 || actor.getClase() != SANADOR || habilidad.costePm() == 0)
                    .max(Comparator.comparingInt(habilidad -> CalculoCombate.danio(actor, objetivo, habilidad, false)))
                    .orElseThrow();
            actuar(partida, actor, ataque.id(), objetivo);
        }
    }

    private static void prepararEquipo(Partida partida) {
        var arquero = partida.getEquipo().getFirst();
        var sanador = partida.getEquipo().get(1);
        var guerrero = partida.getEquipo().getLast();
        int ronda = partida.getRonda();

        invertir(partida, arquero, FUE, Math.max(0, 15 - arquero.getEstadisticasBase().fuerza()));
        invertir(partida, arquero, ronda < 5 ? PV : FUE, arquero.getPuntos());
        invertir(partida, sanador, INT, Math.max(0, 16 - sanador.getEstadisticasBase().inteligencia()));
        invertir(partida, sanador, PV, sanador.getPuntos());
        invertir(partida, guerrero, FUE, guerrero.getPuntos());

        comprar(partida, "arco_largo", arquero);
        if (ronda >= 3) comprar(partida, "espada_pesada", guerrero);
        if (ronda >= 4) comprar(partida, "tunica_mago", sanador);
        if (ronda >= 8) {
            comprar(partida, "pesada", guerrero);
            comprar(partida, "proteccion", sanador);
        }
        if (ronda == 9) comprar(partida, "tormenta", sanador);
        for (Heroe heroe : partida.getEquipo()) {
            if (heroe.estaVivo() && vidaFaltante(heroe) >= 40 && comprar(partida, "azul", heroe)) {
                usarSiExiste(partida, heroe, buscar(partida, "azul"), heroe);
            }
        }
        reponer(partida, "roja", ronda >= 8 ? 5 : 2, sanador);
        reponer(partida, "verde", ronda >= 8 ? 2 : 1, sanador);
        if (ronda >= 6 && partida.getOro() >= 180) {
            comprar(partida, "guantes_reforzados", guerrero);
            comprar(partida, "botas", arquero);
        }
    }

    private static int prioridadEnemigo(Combatiente enemigo) {
        var tipo = ((Enemigo) enemigo).getTipo();
        return enemigo.getPvActuales() - (tipo == TipoEnemigo.CHAMAN ? 120 : 0);
    }

    private static int vidaFaltante(Heroe heroe) {
        return heroe.getEstadisticasBase().pvMaximos() - heroe.getPvActuales();
    }

    private static Optional<Objeto> buscar(Partida partida, String codigo) {
        return partida.getInventario().stream().filter(objeto -> objeto.codigo().equals(codigo)).findFirst();
    }

    private static void actuar(Partida partida, Heroe actor, String habilidad, Combatiente objetivo) {
        partida.actuar(new AccionCombate(partida.getCombate().getTurno(), actor.getId(), habilidad, objetivo.getId()));
    }

    private static boolean usarSiExiste(Partida partida, Heroe actor, Optional<Objeto> objeto, Combatiente objetivo) {
        if (objeto.isEmpty()) {
            return false;
        }
        partida.usarConsumible(partida.getCombate().getTurno(), actor.getId(), objeto.get().id(),
                objetivo == null ? null : objetivo.getId());
        return true;
    }

    private static void invertir(Partida partida, Heroe heroe, Atributo atributo, int maximo) {
        int cantidad = Math.min(maximo, heroe.getPuntos());
        if (cantidad > 0) {
            partida.invertir(heroe.getId(), atributo, cantidad);
        }
    }

    private static void reponer(Partida partida, String codigo, int cantidad, Heroe comprador) {
        long disponibles = partida.getInventario().stream().filter(objeto -> objeto.codigo().equals(codigo)).count();
        for (long unidad = disponibles; unidad < cantidad; unidad++) {
            if (!comprar(partida, codigo, comprador)) {
                return;
            }
        }
    }

    private static boolean comprar(Partida partida, String codigo, Heroe comprador) {
        var oferta = partida.getOfertas().stream()
                .filter(entrada -> entrada.objeto().id() == null && entrada.objeto().codigo().equals(codigo))
                .findFirst().orElseThrow();
        var plantilla = oferta.objeto();
        if (partida.getOro() < oferta.precio() || Objects.equals(0, oferta.existencias())
                || !plantilla.cumpleRequisitos(comprador)) {
            return false;
        }
        var equipado = plantilla.ranura() == null ? null : comprador.getEquipo().get(plantilla.ranura());
        if (equipado != null && equipado.codigo().equals(codigo)) {
            return false;
        }
        partida.comprar(codigo, null);
        if (plantilla.categoria().esEquipo()) {
            partida.equipar(comprador.getId(), partida.getInventario().getLast().id());
        }
        return true;
    }
}
