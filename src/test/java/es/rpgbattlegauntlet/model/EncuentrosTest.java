package es.rpgbattlegauntlet.model;

import es.rpgbattlegauntlet.config.CatalogoEncuentros;
import es.rpgbattlegauntlet.config.Equilibrio;
import es.rpgbattlegauntlet.enums.Dificultad;
import es.rpgbattlegauntlet.enums.EstadoPartida;
import es.rpgbattlegauntlet.enums.TipoEnemigo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static es.rpgbattlegauntlet.enums.ClaseHeroe.*;
import static es.rpgbattlegauntlet.enums.TipoEnemigo.*;
import static org.junit.jupiter.api.Assertions.*;

class EncuentrosTest {

    private static final List<List<TipoEnemigo>> ENCUENTROS_ESPERADOS = List.of(
            List.of(GOBLIN),
            List.of(GOBLIN, GOBLIN_ARQUERO),
            List.of(GOBLIN, ESQUELETO),
            List.of(ESQUELETO, CHAMAN, GOBLIN_ARQUERO),
            List.of(OGRO),
            List.of(ESQUELETO, ESQUELETO, CHAMAN),
            List.of(CABALLERO_OSCURO),
            List.of(CABALLERO_OSCURO, CHAMAN),
            List.of(CABALLERO_OSCURO, CABALLERO_OSCURO),
            List.of(DRAGON)
    );

    @Test
    void catalogoRechazaRondasFueraDelIntervalo() {
        assertEquals(10, CatalogoEncuentros.totalRondas());
        assertThrows(IllegalArgumentException.class, () -> CatalogoEncuentros.crearEnemigos(0));
        assertThrows(IllegalArgumentException.class, () -> CatalogoEncuentros.crearEnemigos(11));
    }

    @Test
    void dificilExpertoYPruebasAmplianLaCampanaYElDragonEsElUltimoEncuentroPublico() {
        for (Dificultad dificultad : Dificultad.values()) {
            int esperado = dificultad == Dificultad.FACIL ? 10 : 15;
            assertEquals(esperado, CatalogoEncuentros.totalRondas(dificultad));
            assertEquals(List.of(DRAGON), CatalogoEncuentros.crearEnemigos(esperado, dificultad).stream()
                    .map(Enemigo::getTipo).toList());
            assertThrows(IllegalArgumentException.class,
                    () -> CatalogoEncuentros.crearEnemigos(esperado + 1, dificultad));
        }
    }

    @ParameterizedTest
    @EnumSource(value = Dificultad.class, names = {"DIFICIL", "EXPERTO", "PRUEBAS"})
    void campanaAmpliadaTerminaConElDragonEnLaRonda15(Dificultad dificultad) {
        var partida = partidaSinCriticosNiEsquivas();
        partida.seleccionarHeroes(List.of(PICARO, ARQUERO, GUERRERO));
        partida.iniciarCombate(dificultad);
        var tramoAmpliado = List.of(
                List.of(VERDUGO_ESPECTRAL),
                List.of(NIGROMANTE, CABALLERO_OSCURO),
                List.of(GOLEM_RUNICO),
                List.of(NIGROMANTE, NIGROMANTE),
                List.of(CABALLERO_OSCURO, CABALLERO_OSCURO, NIGROMANTE),
                List.of(DRAGON));
        assertEquals(15, partida.getTotalRondas());

        for (int ronda = 1; ronda <= 15; ronda++) {
            var esperados = ronda < 10 ? ENCUENTROS_ESPERADOS.get(ronda - 1) : tramoAmpliado.get(ronda - 10);
            var enemigos = partida.getCombate().getEnemigos();
            assertEquals(esperados, enemigos.stream().map(Enemigo.class::cast).map(Enemigo::getTipo).toList());
            int oroAntes = partida.getOro();

            ganarEncuentroDePrueba(partida);

            if (ronda < 15) {
                assertEquals(EstadoPartida.ENTRE_COMBATES, partida.getEstado());
                assertEquals(ronda, partida.getRecompensa().ronda());
                if (dificultad == Dificultad.PRUEBAS) {
                    assertTrue(partida.tieneOroIlimitado());
                    assertEquals(oroAntes, partida.getOro());
                } else {
                    assertTrue(partida.getOro() > oroAntes);
                }
                if (ronda == 14) {
                    assertEquals(371, partida.getRecompensa().experiencia());
                    for (var heroe : partida.getEquipo()) {
                        assertEquals(12, heroe.getNivel());
                        assertEquals(0, heroe.getExperiencia());
                        assertEquals(35, heroe.getPuntos());
                    }
                }
                partida.getEquipo().forEach(heroe -> heroe.recibirCuracion(heroe.getEstadisticasBase().pvMaximos()));
                partida.avanzarRonda(ronda);
            } else {
                assertEquals(EstadoPartida.VICTORIA, partida.getEstado());
                assertNull(partida.getRecompensa());
                assertEquals(oroAntes, partida.getOro());
            }
        }
        assertThrows(IllegalStateException.class, () -> partida.avanzarRonda(15));
    }

    @Test
    void dificilAumentaLaVidaYMultiplicaElDanioPorUnoYMedio() {
        var goblin = CatalogoEncuentros.crearEnemigos(1, Dificultad.DIFICIL).getFirst();
        assertEquals(81, goblin.getPvActuales());
        assertEquals(1.5, goblin.getMultiplicadorDanio());
    }

    @ParameterizedTest
    @CsvSource({"EXPERTO,true", "EXPERTO,false", "PRUEBAS,true", "PRUEBAS,false"})
    void pergaminoCompradoORobadoAbreLaRutaSecreta(Dificultad dificultad, boolean comprar) {
        var forzarRobo = new AtomicBoolean();
        var partida = new Partida(new Random(0) {
            @Override
            public int nextInt(int limite) {
                return forzarRobo.getAndSet(false) ? 0 : limite - 1;
            }
        });
        partida.seleccionarHeroes(List.of(PICARO, MAGO, GUERRERO));
        partida.iniciarCombate(dificultad);
        var mago = partida.getEquipo().get(1);
        for (int ronda = 1; ronda <= 14; ronda++) {
            ganarEncuentroDePrueba(partida);
            assertEquals(15, partida.getTotalRondas());
            var oferta = partida.getOfertas().stream().filter(o -> o.objeto().codigo().equals("requiem_dragon")).findFirst();
            assertEquals(ronda == 14, oferta.isPresent());
            if (ronda == 14) {
                partida.invertir(mago.getId(), es.rpgbattlegauntlet.enums.Atributo.INT, 4);
                partida.desequipar(partida.getEquipo().getFirst().getId(), es.rpgbattlegauntlet.enums.RanuraEquipo.CABEZA);
                if (comprar) {
                    assertEquals(1000, oferta.orElseThrow().precio());
                    int oro = partida.getOro();
                    partida.comprar("requiem_dragon", null);
                    assertEquals(dificultad == Dificultad.PRUEBAS ? oro : oro - 1000, partida.getOro());
                    // El grupo se defiende mientras espera la llegada de los draconianos.
                    for (var heroe : partida.getEquipo()) {
                        if (heroe.getClase() == PICARO || heroe.getClase() == GUERRERO) {
                            heroe.equipar(es.rpgbattlegauntlet.config.CatalogoObjetos.equipo(
                                    "escudo_magico", es.rpgbattlegauntlet.enums.Calidad.NORMAL, false));
                        }
                    }
                }
            }
            partida.getEquipo().forEach(heroe -> heroe.recibirCuracion(heroe.getEstadisticasBase().pvMaximos()));
            partida.avanzarRonda(ronda);
        }
        var combate = partida.getCombate();
        var azrath = combate.getEnemigos().getFirst();
        var capucha = partida.getInventario().stream().filter(objeto -> objeto.codigo().equals("capucha")).findFirst().orElseThrow();
        int turno = combate.getTurno();
        var actorActual = combate.getOrdenActuacion().getFirst();
        var error = assertThrows(IllegalArgumentException.class,
                () -> partida.usarConsumible(turno, actorActual.getId(), capucha.id(), azrath.getId()));
        assertEquals("El objeto seleccionado no es un consumible", error.getMessage());
        assertEquals(turno, combate.getTurno());
        assertEquals(15, partida.getRonda());
        assertTrue(partida.getInventario().contains(capucha));
        if (!comprar) {
            var picaro = partida.getEquipo().getFirst();
            assertSame(picaro, combate.getOrdenActuacion().getFirst());
            forzarRobo.set(true);
            partida.actuar(new AccionCombate(combate.getTurno(), picaro.getId(), "robar", azrath.getId()));
        }
        while (combate.getOrdenActuacion().getFirst() != mago) {
            var actor = combate.getOrdenActuacion().getFirst();
            partida.actuar(new AccionCombate(combate.getTurno(), actor.getId(), "defender", null));
        }
        var pergamino = partida.getInventario().stream().filter(o -> o.codigo().equals("requiem_dragon")).findFirst().orElseThrow();
        if (dificultad == Dificultad.EXPERTO) azrath.recibirDanio(azrath.getPvActuales() - 301);
        int pvAlCruzar = Math.max(1, azrath.getPvActuales() / 2);
        int accionesPrevias = ((Enemigo) azrath).getAccionesRealizadas();
        var pvHeroes = partida.getEquipo().stream().map(Heroe::getPvActuales).toList();
        var pcHeroes = partida.getEquipo().stream().map(Heroe::getPmActuales).toList();
        partida.usarConsumible(combate.getTurno(), mago.getId(), pergamino.id(), azrath.getId());
        var historialAnterior = combate.getHistorial();
        assertEquals("dialogo_pergamino", historialAnterior.get(historialAnterior.size() - 2).habilidad());
        assertEquals("traslado_azrath", historialAnterior.getLast().habilidad());
        assertEquals(20, partida.getTotalRondas());
        assertEquals(16, partida.getRonda());
        assertEquals(EstadoPartida.COMBATE, partida.getEstado());
        var reino = partida.getCombate();
        assertEquals(List.of(azrath), reino.getEnemigos());
        assertEquals(pvAlCruzar, azrath.getPvActuales());
        assertEquals(pvHeroes, partida.getEquipo().stream().map(Heroe::getPvActuales).toList());
        assertEquals(pcHeroes, partida.getEquipo().stream().map(Heroe::getPmActuales).toList());

        if (comprar) {
            while (reino.getEnemigos().size() == 1) {
                assertTrue(((Enemigo) azrath).getAccionesRealizadas() - accionesPrevias < 2);
                var actor = reino.getOrdenActuacion().getFirst();
                partida.actuar(new AccionCombate(reino.getTurno(), actor.getId(), "defender", null));
            }
            assertEquals(2, ((Enemigo) azrath).getAccionesRealizadas() - accionesPrevias);
        } else {
            azrath.recibirDanio(azrath.getPvActuales() - 1);
            var actor = reino.getOrdenActuacion().getFirst();
            partida.actuar(new AccionCombate(reino.getTurno(), actor.getId(), "ataque_basico", azrath.getId()));
            assertFalse(azrath.estaVivo());
            assertEquals(EstadoPartida.COMBATE, partida.getEstado());
        }
        assertEquals(List.of(DRAGON, DRACONIANO_CIMITARRA, DRACONIANO_BALLESTA),
                reino.getEnemigos().stream().map(Enemigo.class::cast).map(Enemigo::getTipo).toList());
        assertEquals(1, reino.getHistorial().stream().filter(e -> e.habilidad().equals("llegan_draconianos")).count());
        assertFalse(partida.getOfertas().stream().anyMatch(o -> o.objeto().codigo().equals("ballesta")));
        ganarEncuentroDePrueba(partida);
        assertEquals(List.of("cimitarra_draconica", "ballesta"), partida.getBotin().stream().map(Objeto::codigo).toList());
        assertTrue(partida.getOfertas().stream().anyMatch(o -> o.objeto().codigo().equals("cazadragones")));
        assertTrue(partida.getOfertas().stream().anyMatch(o -> o.objeto().codigo().equals("ventisca")));
        assertTrue(partida.getOfertas().stream().anyMatch(o -> o.objeto().codigo().equals("ballesta")
                && o.objeto().calidad() == es.rpgbattlegauntlet.enums.Calidad.SUPERIOR));
        assertTrue(partida.getOfertas().stream().anyMatch(o -> o.objeto().codigo().equals("cimitarra_draconica")));
        var encuentros = List.of(List.of(GUIVERNO),
                List.of(DRAGON_ESCARCHA), List.of(DRAGON_OBSIDIANA), List.of(DIOS_DRAGON));
        for (int ronda = 17; ronda <= 20; ronda++) {
            partida.getEquipo().forEach(heroe -> heroe.recibirCuracion(heroe.getEstadisticasBase().pvMaximos()));
            var encuentro = partida.avanzarRonda(ronda - 1);
            assertEquals(encuentros.get(ronda - 17), encuentro.getEnemigos().stream()
                    .map(Enemigo.class::cast).map(Enemigo::getTipo).toList());
            if (dificultad == Dificultad.PRUEBAS) {
                assertTrue(encuentro.getEnemigos().stream().allMatch(enemigo -> enemigo.getPvActuales() == 1));
            }
            ganarEncuentroDePrueba(partida);
            var botin = switch (ronda) {
                case 17 -> List.of("aguijon_riscos");
                case 18 -> List.of("ventisca", "cetro_invierno", "armadura_escarcha");
                case 19 -> List.of("espada_obsidiana", "armadura_obsidiana");
                default -> List.of();
            };
            assertEquals(botin, partida.getBotin().stream().map(Objeto::codigo).toList());
            if (ronda == 19 && dificultad == Dificultad.PRUEBAS) {
                assertEquals(396, partida.getRecompensa().experiencia());
                for (var heroe : partida.getEquipo()) {
                    assertEquals(15, heroe.getNivel());
                    assertEquals(0, heroe.getExperiencia());
                }
            }
        }
        assertEquals(EstadoPartida.VICTORIA, partida.getEstado());
    }

    @Test
    void pruebasPermiteEquiparYUsarPergaminosSinAtributosMinimos() {
        var partida = partidaSinCriticosNiEsquivas();
        partida.seleccionarHeroes(List.of(PICARO, MAGO, GUERRERO));
        partida.iniciarCombate(Dificultad.PRUEBAS, 14);
        ganarEncuentroDePrueba(partida);
        var mago = partida.getEquipo().get(1);
        var guerrero = partida.getEquipo().getLast();
        var picaro = partida.getEquipo().getFirst();
        partida.comprar("vara_avanzada", null);
        var vara = partida.getInventario().stream().filter(o -> o.codigo().equals("vara_avanzada")).findFirst().orElseThrow();
        assertThrows(IllegalArgumentException.class, () -> partida.equipar(picaro.getId(), vara.id()));
        assertFalse(vara.cumpleRequisitos(mago));
        partida.equipar(mago.getId(), vara.id());
        assertTrue(mago.getEquipo().containsValue(vara));
        partida.comprar("espada_pesada", null);
        var espada = partida.getInventario().stream().filter(o -> o.codigo().equals("espada_pesada")).findFirst().orElseThrow();
        assertFalse(espada.cumpleRequisitos(guerrero));
        assertThrows(IllegalArgumentException.class, () -> partida.equipar(mago.getId(), espada.id()));
        partida.equipar(guerrero.getId(), espada.id());
        assertTrue(guerrero.getEquipo().containsValue(espada));

        partida.comprar("requiem_dragon", null);
        var pergamino = partida.getInventario().stream().filter(o -> o.codigo().equals("requiem_dragon")).findFirst().orElseThrow();
        var combate = partida.avanzarRonda(14);
        var actor = (Heroe) combate.getOrdenActuacion().getFirst();
        assertFalse(pergamino.cumpleRequisitos(actor));
        partida.usarConsumible(combate.getTurno(), actor.getId(), pergamino.id(), combate.getEnemigos().getFirst().getId());
        assertEquals(16, partida.getRonda());
    }

    @Test
    void pruebasMuestraElCosteDeLasHabilidadesPeroSoloLosEnemigosGastanPc() {
        var mago = es.rpgbattlegauntlet.config.CatalogoHeroes.crearHeroe(MAGO);
        var chaman = new Enemigo(CHAMAN, new EstadisticasBase(300, 80, 5, 5, 14, 12, 8, 5, 5));
        var combate = new Combate(List.of(mago), List.of(chaman), new Random(3), 1, true);
        mago.consumirMagia(mago.getPmActuales());
        mago.aplicarEstado(es.rpgbattlegauntlet.enums.EstadoTemporal.ESCARCHA, 1);

        var fuego = combate.getHabilidadesDisponibles().stream().filter(h -> h.id().equals("bola_fuego")).findFirst().orElseThrow();
        assertEquals(13, fuego.costePm());
        combate.ejecutar(new AccionCombate(combate.getTurno(), mago.getId(), fuego.id(), chaman.getId()));
        assertEquals(0, mago.getPmActuales());
        assertEquals(70, chaman.getPmActuales());
    }

    @Test
    void modoPruebasEvitaDanioFisicoMagicoYPeriodicoEnLosHeroes() {
        var partida = partidaSinCriticosNiEsquivas();
        partida.seleccionarHeroes(List.of(PICARO, ARQUERO, GUERRERO));
        var inicial = partida.iniciarCombate(Dificultad.PRUEBAS);
        for (int i = 0; i < 2; i++) {
            var actor = inicial.getOrdenActuacion().getFirst();
            partida.actuar(new AccionCombate(inicial.getTurno(), actor.getId(), "defender", null));
        }
        var golpe = inicial.getHistorial().stream().filter(evento -> evento.habilidad().equals("cuchillada")).findFirst().orElseThrow();
        assertEquals(0, golpe.danio());
        assertTrue(partida.getEquipo().stream().allMatch(h -> h.getPvActuales() == h.getEstadisticasBase().pvMaximos()));

        var heroe = partida.getEquipo().getLast();
        var chaman = es.rpgbattlegauntlet.config.CatalogoEnemigos.crear(CHAMAN, Dificultad.PRUEBAS);
        var combate = new Combate(List.of(heroe), List.of(chaman), new Random(3), 1, true);
        assertEquals("fuego_chaman", combate.getHistorial().getFirst().habilidad());
        assertEquals(0, combate.getHistorial().getFirst().danio());
        heroe.aplicarEstado(es.rpgbattlegauntlet.enums.EstadoTemporal.ENVENENADO, 1);
        heroe.aplicarEstado(es.rpgbattlegauntlet.enums.EstadoTemporal.QUEMADO, 1);
        combate.ejecutar(new AccionCombate(combate.getTurno(), heroe.getId(), "defender", null));
        for (String estado : List.of("veneno", "quemadura")) {
            var evento = combate.getHistorial().stream().filter(e -> e.habilidad().equals(estado)).findFirst().orElseThrow();
            assertEquals(0, evento.danio());
        }
        assertEquals(heroe.getEstadisticasBase().pvMaximos(), heroe.getPvActuales());
    }

    @Test
    void modoPruebasCreaTodosLosEnemigosConUnPuntoDeVida() {
        for (int ronda = 1; ronda <= CatalogoEncuentros.totalRondas(Dificultad.PRUEBAS); ronda++) {
            for (var enemigo : CatalogoEncuentros.crearEnemigos(ronda, Dificultad.PRUEBAS)) {
                assertEquals(1, enemigo.getPvActuales());
                assertEquals(1, enemigo.getEstadisticasBase().pvMaximos());
                assertEquals(1.0, enemigo.getMultiplicadorDanio());
            }
        }
    }

    @Test
    void recorreLasDiezRondasEnOrdenYTerminaEnVictoria() {
        var partida = partidaSinCriticosNiEsquivas();
        assertEquals(0, partida.getRonda());
        assertNull(partida.getCombate());
        assertThrows(IllegalStateException.class, partida::iniciarCombate);
        partida.seleccionarHeroes(List.of(PICARO, ARQUERO, GUERRERO));
        var combate = partida.iniciarCombate();
        assertThrows(IllegalStateException.class, partida::iniciarCombate);
        assertThrows(IllegalStateException.class, () -> partida.seleccionarHeroes(List.of(MAGO, PICARO, GUERRERO)));
        var idsEnemigos = new HashSet<UUID>();
        int ultimoTurno = 0;

        for (int ronda = 1; ronda <= ENCUENTROS_ESPERADOS.size(); ronda++) {
            int numero = ronda;
            assertEquals(numero, partida.getRonda());
            assertSame(combate, partida.getCombate());
            assertEquals(EstadoPartida.COMBATE, partida.getEstado());
            assertTrue(combate.getTurno() > ultimoTurno);
            assertThrows(IllegalStateException.class, () -> partida.avanzarRonda(numero));
            assertEquals(ENCUENTROS_ESPERADOS.get(numero - 1), combate.getEnemigos().stream()
                    .map(Enemigo.class::cast).map(Enemigo::getTipo).toList());

            for (var enemigo : combate.getEnemigos()) {
                assertTrue(idsEnemigos.add(enemigo.getId()));
                assertEquals(enemigo.getEstadisticasBase().pvMaximos(), enemigo.getPvActuales());
            }
            ganarEncuentroDePrueba(partida);
            ultimoTurno = combate.getTurno();
            if (numero < ENCUENTROS_ESPERADOS.size()) {
                assertEquals(EstadoPartida.ENTRE_COMBATES, partida.getEstado());
                assertThrows(IllegalStateException.class, () -> partida.avanzarRonda(numero + 1));
                combate = partida.avanzarRonda(numero);
            }
        }

        assertEquals(EstadoPartida.VICTORIA, partida.getEstado());
        assertEquals(10, partida.getRonda());
        assertNull(partida.getRecompensa());
        assertTrue(partida.getBotin().isEmpty());
        assertThrows(IllegalStateException.class, () -> partida.avanzarRonda(10));
        assertThrows(IllegalStateException.class, partida::iniciarCombate);
    }

    @Test
    void conservaPvPmEIdentidadYRechazaAccionesDeLaRondaAnterior() {
        var partida = partidaSinCriticosNiEsquivas();
        partida.seleccionarHeroes(List.of(PICARO, ARQUERO, GUERRERO));
        var primero = partida.iniciarCombate();
        var equipo = partida.getEquipo();
        var picaro = equipo.getFirst();
        var arquero = equipo.get(1);
        var guerrero = equipo.getLast();
        picaro.recibirDanio(15);
        partida.actuar(new AccionCombate(primero.getTurno(), picaro.getId(), "esquivar", null));
        partida.actuar(new AccionCombate(primero.getTurno(), arquero.getId(), "defender", null));
        arquero.recibirDanio(arquero.getPvActuales());
        int pvTrasElCombate = picaro.getPvActuales();
        var enemigo = primero.getEnemigos().getFirst();
        enemigo.recibirDanio(enemigo.getPvActuales() - 1);
        var accionAnterior = new AccionCombate(primero.getTurno(), guerrero.getId(), "ataque_basico", enemigo.getId());
        partida.actuar(accionAnterior);

        assertEquals(2, partida.getRecompensa().supervivientes());
        assertEquals(2, picaro.getNivel());
        assertEquals(2, guerrero.getNivel());
        assertEquals(1, arquero.getNivel());
        assertEquals(0, arquero.getExperiencia());
        assertEquals(0, arquero.getPuntos());

        var segundo = partida.avanzarRonda(1);

        assertEquals(equipo, partida.getEquipo());
        assertEquals(Math.min(85, pvTrasElCombate + Equilibrio.PV_AL_SUBIR), picaro.getPvActuales());
        assertEquals(25, picaro.getPmActuales());
        assertFalse(arquero.estaVivo());
        assertFalse(segundo.getOrdenActuacion().contains(arquero));
        assertTrue(picaro.getEstados().isEmpty());
        assertThrows(IllegalStateException.class, () -> partida.avanzarRonda(1));
        assertThrows(IllegalArgumentException.class, () -> segundo.ejecutar(
                new AccionCombate(accionAnterior.turno(), picaro.getId(), "defender", null)));
        ganarEncuentroDePrueba(partida);
        assertThrows(IllegalStateException.class, () -> partida.avanzarRonda(1));
    }

    @Test
    void derrotaImpideAvanzar() {
        var partida = partidaSinCriticosNiEsquivas();
        partida.seleccionarHeroes(List.of(GUERRERO, MAGO, SANADOR));
        partida.getEquipo().forEach(heroe -> heroe.recibirDanio(heroe.getPvActuales() - 1));
        var combate = partida.iniciarCombate();

        while (partida.getEstado() == EstadoPartida.COMBATE) {
            var actor = combate.getOrdenActuacion().getFirst();
            combate.ejecutar(new AccionCombate(combate.getTurno(), actor.getId(), "defender", null));
        }

        assertEquals(EstadoPartida.DERROTA, partida.getEstado());
        assertEquals(1, partida.getRonda());
        assertThrows(IllegalStateException.class, () -> partida.avanzarRonda(1));
        assertThrows(IllegalStateException.class, partida::iniciarCombate);
    }

    private static Partida partidaSinCriticosNiEsquivas() {
        return new Partida(new Random(0) {
            @Override
            public int nextInt(int limite) {
                return limite - 1;
            }
        });
    }

    private static void ganarEncuentroDePrueba(Partida partida) {
        var combate = partida.getCombate();
        // Se aísla el avance de la campaña con enemigos a 1 PV y se evita que el nigromante los reanime.
        var ordenObjetivos = new ArrayList<>(combate.getEnemigos());
        ordenObjetivos.sort(Comparator.comparingInt(enemigo -> ((Enemigo) enemigo).getTipo() == NIGROMANTE ? 0 : 1));
        int acciones = 0;
        while (partida.getEstado() == EstadoPartida.COMBATE) {
            assertTrue(++acciones <= ordenObjetivos.size() * 3, "El combate de prueba debe terminar aunque haya reanimaciones");
            var enemigo = ordenObjetivos.stream().filter(Combatiente::estaVivo).findFirst().orElseThrow();
            var actor = combate.getOrdenActuacion().getFirst();
            enemigo.recibirDanio(enemigo.getPvActuales() - 1);
            partida.actuar(new AccionCombate(combate.getTurno(), actor.getId(), "ataque_basico", enemigo.getId()));
        }
    }
}
