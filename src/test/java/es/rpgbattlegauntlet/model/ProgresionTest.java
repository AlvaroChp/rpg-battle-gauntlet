package es.rpgbattlegauntlet.model;

import es.rpgbattlegauntlet.algorithm.GeneradorBotin;
import es.rpgbattlegauntlet.config.CatalogoObjetos;
import es.rpgbattlegauntlet.config.CatalogoEnemigos;
import es.rpgbattlegauntlet.config.CatalogoHeroes;
import es.rpgbattlegauntlet.config.Equilibrio;
import es.rpgbattlegauntlet.enums.EstadoTemporal;
import es.rpgbattlegauntlet.enums.ClaseHeroe;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Random;

import static es.rpgbattlegauntlet.enums.Atributo.FUE;
import static es.rpgbattlegauntlet.enums.Atributo.DES;
import static es.rpgbattlegauntlet.enums.Calidad.*;
import static es.rpgbattlegauntlet.enums.Categoria.*;
import static es.rpgbattlegauntlet.enums.ClaseHeroe.*;
import static es.rpgbattlegauntlet.enums.Dificultad.PRUEBAS;
import static es.rpgbattlegauntlet.enums.PotenciaPergamino.PODEROSO;
import static es.rpgbattlegauntlet.enums.TipoEnemigo.*;
import static org.junit.jupiter.api.Assertions.*;

class ProgresionTest {

    @Test
    void equipoInicialAplicaBonosYSoloLosAtributosBaseCumplenRequisitos() {
        var partida = trasPrimerEncuentro();
        var guerrero = partida.getEquipo().getFirst();
        var arquero = partida.getEquipo().get(1);
        var mago = partida.getEquipo().getLast();
        assertEquals(19, guerrero.getEstadisticasBase().fuerza() + guerrero.getBonificaciones().fuerza());
        assertEquals(23, guerrero.getDefensaEfectiva());
        assertEquals(3, guerrero.getVelocidadEfectiva());
        assertEquals(3, mago.getBonificaciones().potenciaMagica());

        partida.comprar("arco_largo", null);
        var arco = objeto(partida, "arco_largo");
        assertThrows(IllegalArgumentException.class, () -> partida.equipar(arquero.getId(), arco.id()));
        assertEquals(arco, objeto(partida, "arco_largo"));
        assertThrows(IllegalArgumentException.class, () -> partida.invertir(arquero.getId(), FUE, 4));
        partida.invertir(arquero.getId(), FUE, 2);
        partida.equipar(arquero.getId(), arco.id());
        assertEquals(arco, arquero.getEquipo().get(ARMAS));
        assertEquals(1, arquero.getPuntos());
        var inicial = objeto(partida, "arco_corto");
        int oro = partida.getOro();
        partida.vender(inicial.id());
        assertEquals(oro + inicial.precioVenta(), partida.getOro());
        assertFalse(partida.getInventario().contains(inicial));
        assertTrue(partida.getOfertas().stream().anyMatch(oferta -> oferta.objeto() == inicial));
        partida.desequipar(arquero.getId(), ARMADURAS);
        var armaduraInicial = objeto(partida, "ligera_basica");
        oro = partida.getOro();
        partida.vender(armaduraInicial.id());
        assertEquals(oro + armaduraInicial.precioVenta(), partida.getOro());
        assertFalse(partida.getInventario().contains(armaduraInicial));
        assertTrue(partida.getOfertas().stream().anyMatch(oferta -> oferta.objeto() == armaduraInicial));
        assertThrows(IllegalArgumentException.class, () -> partida.desequipar(arquero.getId(), ARMAS));
        assertSame(arco, arquero.getEquipo().get(ARMAS));
        assertEquals("flecha_de_alcance", arquero.habilidadArma().id());

        partida.comprar("daga", null);
        var daga = objeto(partida, "daga");
        assertThrows(IllegalArgumentException.class, () -> partida.equipar(arquero.getId(), daga.id()));
        assertSame(daga, objeto(partida, "daga"));
        assertSame(arco, arquero.getEquipo().get(ARMAS));
    }

    @Test
    void familiasDeArmasRestringenLaClaseAunqueCumplaLosAtributos() {
        var stats = new EstadisticasBase(100, 100, 30, 15, 30, 15, 10, 5, 5);
        for (var clase : ClaseHeroe.values()) {
            var heroe = new Heroe(clase, stats);
            for (String codigo : List.of("espada_pesada", "daga", "garrote")) {
                assertEquals(clase == GUERRERO || clase == PICARO,
                        CatalogoObjetos.equipo(codigo, NORMAL, false).cumpleRequisitos(heroe));
            }
            for (String codigo : List.of("arco_largo", "ballesta")) {
                assertEquals(clase == ARQUERO, CatalogoObjetos.equipo(codigo, NORMAL, false).cumpleRequisitos(heroe));
            }
            assertEquals(clase == MAGO || clase == SANADOR,
                    CatalogoObjetos.equipo("vara_avanzada", NORMAL, false).cumpleRequisitos(heroe));
        }
    }

    @Test
    void cadaArmaTieneSuPropioAtaqueYLoConservaEnTodasLasCalidades() {
        var codigos = java.util.stream.Stream.concat(
                CatalogoObjetos.tienda().stream().filter(objeto -> objeto.categoria() == ARMAS).map(Objeto::codigo),
                java.util.stream.Stream.of("espada_esqueleto", "maldita", "vara_chaman", "arco_goblin")).toList();
        var habilidades = new java.util.HashSet<String>();
        for (String codigo : codigos) {
            var habilidad = CatalogoObjetos.equipo(codigo, NORMAL, false).habilidad();
            assertTrue(habilidades.add(habilidad.id()), codigo);
            assertEquals(habilidad, CatalogoObjetos.equipo(codigo, INFERIOR, false).habilidad());
            assertEquals(habilidad, CatalogoObjetos.equipo(codigo, SUPERIOR, false).habilidad());
        }
    }

    @Test
    void filoDeLaMaldicionInfligeDanioYDebilitaLaDefensa() {
        var guerrero = new Heroe(GUERRERO, new EstadisticasBase(150, 20, 20, 15, 3, 8, 6, 5, 5));
        guerrero.equipar(CatalogoObjetos.equipo("maldita", SUPERIOR, false));
        var enemigo = new Combatiente(new EstadisticasBase(100, 0, 1, 15, 0, 0, 1, 0, 0));
        var combate = new Combate(List.of(guerrero), List.of(enemigo), azar());
        combate.ejecutar(new AccionCombate(combate.getTurno(), guerrero.getId(), "filo_maldicion", enemigo.getId()));
        assertEquals(75, combate.getHistorial().getFirst().danio());
        assertEquals(10, enemigo.getDefensaEfectiva());
        assertEquals(14, guerrero.getPmActuales());
    }

    @Test
    void corteAlTendonReduceLaVelocidadAlAcertar() {
        var picaro = new Heroe(PICARO, new EstadisticasBase(85, 25, 14, 6, 4, 7, 17, 20, 15, 25));
        picaro.equipar(CatalogoObjetos.equipo("daga_curva", NORMAL, false));
        var enemigo = new Combatiente(new EstadisticasBase(100, 0, 1, 0, 0, 0, 10, 0, 0));
        var combate = new Combate(List.of(picaro), List.of(enemigo), azar());
        combate.ejecutar(new AccionCombate(combate.getTurno(), picaro.getId(), "corte_al_tendon", enemigo.getId()));
        assertEquals(47, combate.getHistorial().getFirst().danio());
        assertEquals(6, enemigo.getVelocidadEfectiva());
        assertEquals(1, enemigo.getEstados().get(EstadoTemporal.RALENTIZACION));
        assertEquals(21, picaro.getPmActuales());
    }

    @Test
    void ecoDeLaTormentaDaniaYRalentizaSoloALosSupervivientes() {
        var mago = CatalogoHeroes.crearHeroe(MAGO);
        mago.equipar(CatalogoObjetos.equipo("vara_chaman", NORMAL, false));
        var rival = new Combatiente(new EstadisticasBase(100, 0, 1, 0, 0, 0, 8, 0, 100));
        var fragil = new Combatiente(new EstadisticasBase(12, 0, 1, 0, 0, 0, 7, 0, 100));
        var combate = new Combate(List.of(mago), List.of(rival, fragil), azar());

        combate.ejecutar(new AccionCombate(combate.getTurno(), mago.getId(), "eco_de_la_tormenta", null));

        assertEquals(List.of(37, 12), combate.getHistorial().stream().map(EventoCombate::danio).toList());
        assertEquals(4, rival.getVelocidadEfectiva());
        assertEquals(1, rival.getEstados().get(EstadoTemporal.RALENTIZACION));
        assertTrue(fragil.getEstados().isEmpty());
        assertEquals(91, mago.getPmActuales());
    }

    @Test
    void flechaEnvenenadaDaniaAlFinalDeLosDosTurnosDelObjetivo() {
        var arquera = CatalogoHeroes.crearHeroe(ARQUERO);
        arquera.equipar(CatalogoObjetos.equipo("arco_goblin", NORMAL, false));
        var rival = new Combatiente(new EstadisticasBase(100, 0, 1, 0, 0, 0, 8, 0, 0));
        var combate = new Combate(List.of(arquera), List.of(rival), azar(99, 99, 49));
        combate.ejecutar(new AccionCombate(combate.getTurno(), arquera.getId(), "flecha_envenenada", rival.getId()));
        assertEquals(60, rival.getPvActuales());
        assertEquals(2, rival.getEstados().get(EstadoTemporal.ENVENENADO));

        combate.ejecutar(new AccionCombate(combate.getTurno(), rival.getId(), "defender", null));
        assertEquals(56, rival.getPvActuales());
        assertEquals(1, rival.getEstados().get(EstadoTemporal.ENVENENADO));
        combate.ejecutar(new AccionCombate(combate.getTurno(), arquera.getId(), "defender", null));
        assertEquals(56, rival.getPvActuales());
        combate.ejecutar(new AccionCombate(combate.getTurno(), rival.getId(), "defender", null));
        assertEquals(52, rival.getPvActuales());
        assertFalse(rival.getEstados().containsKey(EstadoTemporal.ENVENENADO));
        assertEquals(List.of(4, 4), combate.getHistorial().stream().filter(e -> e.habilidad().equals("veneno")).map(EventoCombate::danio).toList());
    }

    @Test
    void robarFallaEnElLimiteYPermiteReintentarSinDuplicarElObjeto() {
        var picaro = CatalogoHeroes.crearHeroe(PICARO);
        var goblin = CatalogoEnemigos.crear(GOBLIN);
        var combate = new Combate(List.of(picaro), List.of(goblin), azar(70, 99, 99, 69, 0, 35));
        assertNull(combate.ejecutar(new AccionCombate(combate.getTurno(), picaro.getId(), "robar", goblin.getId())));
        assertTrue(combate.puedeSerRobado(goblin));
        var robado = combate.ejecutar(new AccionCombate(combate.getTurno(), picaro.getId(), "robar", goblin.getId()));
        assertEquals("daga", robado.codigo());
        assertFalse(combate.puedeSerRobado(goblin));
        assertThrows(IllegalArgumentException.class, () -> combate.ejecutar(
                new AccionCombate(combate.getTurno(), picaro.getId(), "robar", goblin.getId())));
        assertEquals(15, picaro.getPmActuales());
    }

    @Test
    void armaRobadaEntraEnInventarioYLaDestrezaSubeLaProbabilidadDeRobo() {
        var partida = new Partida(azar(69, 0, 35, 99, 99));
        partida.seleccionarHeroes(List.of(PICARO, ARQUERO, GUERRERO));
        var combate = partida.iniciarCombate(PRUEBAS);
        var goblin = combate.getEnemigos().getFirst();
        var picaro = partida.getEquipo().getFirst();
        partida.actuar(new AccionCombate(combate.getTurno(), picaro.getId(), "robar", goblin.getId()));
        assertSame(objeto(partida, "daga"), partida.getHistorial().getFirst().objetoRobado());
        partida.actuar(new AccionCombate(combate.getTurno(), partida.getEquipo().get(1).getId(), "ataque_basico", goblin.getId()));
        assertTrue(partida.getBotin().isEmpty());
        partida.invertir(picaro.getId(), DES, 3);
        assertEquals(28, picaro.getEstadisticasBase().destreza());
        assertEquals(76, picaro.probabilidadRobo());
    }

    @Test
    void recompraConservaIdentidadYElCatalogoFijoNoSeRepone() {
        var partida = trasPrimerEncuentro();
        partida.comprar("proyectil", null);
        var pergamino = objeto(partida, "proyectil");
        assertThrows(IllegalArgumentException.class, () -> partida.comprar("proyectil", null));

        partida.vender(pergamino.id());
        assertEquals(2, partida.getOfertas().stream().filter(oferta -> oferta.objeto().codigo().equals("proyectil")).count());
        var segundaMano = partida.getOfertas().stream().filter(oferta -> pergamino.id().equals(oferta.objeto().id()))
                .findFirst().orElseThrow();
        assertEquals(pergamino.precioVenta() * 2, segundaMano.precio());
        partida.comprar(null, pergamino.id());
        assertSame(pergamino, objeto(partida, "proyectil"));
        assertThrows(IllegalArgumentException.class, () -> partida.comprar(null, pergamino.id()));
        assertThrows(IllegalArgumentException.class, () -> partida.comprar("proyectil", null));
    }

    @Test
    void pocionesIlimitadasConUnidadesIndependientesYConsumoEnCampamento() {
        var partida = trasPrimerEncuentro();
        var mago = partida.getEquipo().getLast();
        mago.recibirDanio(10);
        mago.consumirMagia(21);
        partida.comprar("azul", null);
        partida.comprar("azul", null);
        assertEquals(2, partida.getInventario().stream().map(Objeto::id).distinct().count());
        var primera = objeto(partida, "azul");
        partida.usarConsumible(0, mago.getId(), primera.id(), mago.getId());
        assertEquals(75, mago.getPvActuales());
        assertEquals(100, mago.getPmActuales());
        assertEquals(1, partida.getInventario().size());
        assertThrows(IllegalArgumentException.class, () -> partida.usarConsumible(0, mago.getId(), primera.id(), mago.getId()));
        assertNull(partida.getOfertas().stream().filter(oferta -> oferta.objeto().codigo().equals("azul"))
                .findFirst().orElseThrow().existencias());
        assertThrows(IllegalArgumentException.class, () -> partida.comprar("vara_avanzada", null));
    }

    @Test
    void pergaminoPideInteligenciaBaseRespetaResistenciaYNoGastaPm() {
        var partida = trasPrimerEncuentro();
        var mago = partida.getEquipo().getLast();
        partida.comprar("ventisca", null);
        var pergamino = objeto(partida, "ventisca");
        var combate = partida.avanzarRonda(1);
        var primerActor = combate.getOrdenActuacion().getFirst();
        var enemigo = combate.getEnemigos().getFirst();

        assertThrows(IllegalArgumentException.class, () -> partida.usarConsumible(combate.getTurno(),
                primerActor.getId(), pergamino.id(), null));
        assertEquals(pergamino, objeto(partida, "ventisca"));
        while (combate.getOrdenActuacion().getFirst() != mago) {
            partida.actuar(new AccionCombate(combate.getTurno(), combate.getOrdenActuacion().getFirst().getId(), "defender", null));
        }
        int turno = combate.getTurno();
        int magia = mago.getPmActuales();
        partida.usarConsumible(turno, mago.getId(), pergamino.id(), null);

        assertEquals(magia, mago.getPmActuales());
        assertEquals(4, enemigo.getPvActuales());
        assertEquals(1, combate.getConsumiblesUsados());
        assertTrue(combate.getTurno() > turno);
        assertTrue(partida.getInventario().isEmpty());
    }

    @Test
    void catalogoTieneCuatroApartadosYCuatroPociones() {
        var ofertas = new Tienda().ofertas();
        assertEquals(4, ofertas.stream().map(oferta -> oferta.objeto().categoria()).distinct().count());
        assertEquals(13, ofertas.stream().filter(oferta -> oferta.objeto().categoria() == ARMAS).count());
        assertEquals(9, ofertas.stream().filter(oferta -> oferta.objeto().categoria() == ARMADURAS).count());
        assertEquals(4, ofertas.stream().filter(oferta -> oferta.objeto().categoria() == POCIONES).count());
        assertEquals(10, ofertas.stream().filter(oferta -> oferta.objeto().categoria() == PERGAMINOS).count());
        assertTrue(ofertas.stream().filter(oferta -> oferta.objeto().categoria() == PERGAMINOS)
                .allMatch(oferta -> Integer.valueOf(1).equals(oferta.existencias())));
    }

    @Test
    void revitalizacionEnCampamentoRequiereUnAliadoCaidoYConsumeUnaUnidad() {
        var partida = trasPrimerEncuentro();
        var vivo = partida.getEquipo().getFirst();
        var caido = partida.getEquipo().get(1);
        partida.comprar("revitalizacion", null);
        var pocion = objeto(partida, "revitalizacion");
        assertThrows(IllegalArgumentException.class,
                () -> partida.usarConsumible(0, vivo.getId(), pocion.id(), caido.getId()));
        assertSame(pocion, objeto(partida, "revitalizacion"));
        caido.recibirDanio(caido.getPvActuales());
        partida.usarConsumible(0, vivo.getId(), pocion.id(), caido.getId());
        assertEquals(25, caido.getPvActuales());
        assertFalse(partida.getInventario().contains(pocion));
    }

    @Test
    void revitalizacionEnCombateDevuelveAlHeroeAlSiguienteCiclo() {
        var mago = CatalogoHeroes.crearHeroe(MAGO);
        var guerrero = CatalogoHeroes.crearHeroe(GUERRERO);
        guerrero.recibirDanio(150);
        var rival = new Combatiente(new EstadisticasBase(100, 0, 1, 0, 0, 0, 1, 0, 0));
        var combate = new Combate(List.of(mago, guerrero), List.of(rival), azar());
        var pocion = CatalogoObjetos.tienda().stream().filter(item -> item.codigo().equals("revitalizacion"))
                .findFirst().orElseThrow().nuevaUnidad(false);
        combate.usarConsumible(combate.getTurno(), mago.getId(), pocion, guerrero.getId());
        assertEquals(37, guerrero.getPvActuales());
        assertEquals(List.of(rival), combate.getOrdenActuacion());
        combate.ejecutar(new AccionCombate(combate.getTurno(), rival.getId(), "defender", null));
        assertEquals(List.of(mago, guerrero, rival), combate.getOrdenActuacion());
    }

    @Test
    void requiemSeRobaAlDragonYLoDerrotaAunqueEsteIleso() {
        var dragon = CatalogoEnemigos.crear(DRAGON);
        var picaro = CatalogoHeroes.crearHeroe(PICARO);
        var mago = new Heroe(MAGO, new EstadisticasBase(75, 100, 4, 5, 22, 14, 16, 5, 5));
        var combate = new Combate(List.of(picaro, mago), List.of(dragon), azar(69));
        var requiem = combate.ejecutar(new AccionCombate(combate.getTurno(), picaro.getId(), "robar", dragon.getId()));
        assertEquals("requiem_dragon", requiem.codigo());
        assertEquals(22, requiem.inteligenciaMinima());
        assertFalse(requiem.cumpleRequisitos(CatalogoHeroes.crearHeroe(MAGO)));
        assertFalse(combate.puedeSerRobado(dragon));
        assertTrue(GeneradorBotin.tirar(DRAGON, azar()).isEmpty());
        assertEquals(650, dragon.getPvActuales());
        combate.usarConsumible(combate.getTurno(), mago.getId(), requiem, dragon.getId());
        assertEquals(0, dragon.getPvActuales());
        assertEquals(650, combate.getHistorial().getLast().danio());
    }

    @Test
    void pocionEnCombateGastaUnaAccionYRegistraMagiaRecuperada() {
        var partida = trasPrimerEncuentro();
        var mago = partida.getEquipo().getLast();
        mago.consumirMagia(55);
        partida.comprar("verde", null);
        var pocion = objeto(partida, "verde");
        var combate = partida.avanzarRonda(1);
        int turno = combate.getTurno();
        var actor = combate.getOrdenActuacion().getFirst();

        partida.usarConsumible(turno, actor.getId(), pocion.id(), mago.getId());

        assertTrue(combate.getTurno() > turno);
        assertEquals(95, mago.getPmActuales());
        assertEquals(1, combate.getConsumiblesUsados());
        assertEquals(50, combate.getHistorial().getFirst().magiaRecuperada());
        assertTrue(partida.getInventario().isEmpty());
    }

    @Test
    void curaTotalPoderosaCuraYProtegeDosTurnos() {
        var mago = new Heroe(MAGO, new EstadisticasBase(75, 100, 4, 5, 21, 14, 9, 5, 5));
        var rival = new Combatiente(new EstadisticasBase(80, 0, 5, 4, 0, 2, 1, 0, 0));
        var combate = new Combate(List.of(mago), List.of(rival), azar());
        mago.recibirDanio(60);
        Objeto pergamino = CatalogoObjetos.pergamino("cura_total", PODEROSO);

        combate.usarConsumible(1, mago.getId(), pergamino, mago.getId());

        assertEquals(75, mago.getPvActuales());
        assertEquals(35, mago.getReduccionDanio());
        assertEquals(2, mago.getEstados().get(EstadoTemporal.PROTECCION_35));
        combate.ejecutar(new AccionCombate(2, rival.getId(), "defender", null));
        combate.ejecutar(new AccionCombate(3, mago.getId(), "defender", null));
        assertEquals(1, mago.getEstados().get(EstadoTemporal.PROTECCION_35));
        combate.ejecutar(new AccionCombate(4, rival.getId(), "defender", null));
        combate.ejecutar(new AccionCombate(5, mago.getId(), "defender", null));
        assertFalse(mago.getEstados().containsKey(EstadoTemporal.PROTECCION_35));
    }

    @Test
    void tiradasSeparanProbabilidadTipoYCalidad() {
        assertTrue(GeneradorBotin.tirar(GOBLIN, azar(35)).isEmpty());
        var daga = GeneradorBotin.tirar(GOBLIN, azar(34, 24, 34)).orElseThrow();
        assertEquals("Daga mellada", daga.nombre());
        assertEquals(INFERIOR, daga.calidad());
        assertEquals(12, daga.precioVenta());
        assertEquals(24, daga.precioRecompra());
        assertEquals("Daga de duelo bruñida", GeneradorBotin.tirar(GOBLIN, azar(34, 25, 85)).orElseThrow().nombre());
        assertEquals("Daga curva astillada", GeneradorBotin.tirar(GOBLIN, azar(34, 44, 34)).orElseThrow().nombre());
        assertTrue(GeneradorBotin.tirar(GOBLIN_ARQUERO, azar(40)).isEmpty());
        assertEquals("Arco goblin reforzado", GeneradorBotin.tirar(GOBLIN_ARQUERO, azar(39, 99, 85)).orElseThrow().nombre());
        var capucha = GeneradorBotin.tirar(GOBLIN, azar(0, 45, 35)).orElseThrow();
        assertEquals(NORMAL, capucha.calidad());
        var oscura = GeneradorBotin.tirar(CABALLERO_OSCURO, azar(74, 40, 85)).orElseThrow();
        assertEquals(SUPERIOR, oscura.calidad());
        assertEquals(-5, oscura.bonificaciones().velocidad());
        assertEquals(20, oscura.fuerzaMinima());
        assertEquals("vara_magica", GeneradorBotin.tirar(CHAMAN, azar(49, 29, 35)).orElseThrow().codigo());
        assertEquals("Vara de chamán ritual", GeneradorBotin.tirar(CHAMAN, azar(49, 30, 85)).orElseThrow().nombre());
        assertEquals("Túnica de chamán ritual", GeneradorBotin.tirar(CHAMAN, azar(49, 65, 85)).orElseThrow().nombre());
        assertEquals("tunica_chaman", GeneradorBotin.elegirBotinRobado(CHAMAN, azar(79, 35)).codigo());
        var meteorito = GeneradorBotin.tirar(CHAMAN, azar(49, 80, 9, 99)).orElseThrow();
        assertEquals(PODEROSO, meteorito.potencia());
        assertEquals(200, meteorito.efecto().cantidad());
        assertEquals(25, meteorito.inteligenciaMinima());
        assertEquals(400, meteorito.precioRecompra());
        assertTrue(GeneradorBotin.tirar(DRAGON, azar()).isEmpty());
    }

    @Test
    void recompensaSeEntregaUnaVezYElBotinSoloSeRecogeUnaVez() {
        var partida = primerEncuentro(azar(99, 99, 0, 0, 35));
        var combate = partida.getCombate();
        var recompensa = partida.getRecompensa();
        assertEquals(Equilibrio.oroRonda(1, 1, 3, 0), recompensa.oro());
        assertEquals(75, recompensa.experiencia());
        assertEquals(2, partida.getEquipo().getFirst().getNivel());
        assertEquals(3, partida.getEquipo().getFirst().getPuntos());
        assertThrows(IllegalStateException.class, () -> partida.actuar(new AccionCombate(
                combate.getTurno(), partida.getEquipo().get(1).getId(), "defender", null)));
        var botin = partida.getBotin().getFirst();
        partida.recoger(botin.id());
        assertThrows(IllegalArgumentException.class, () -> partida.recoger(botin.id()));
        partida.avanzarRonda(1);
        assertEquals(recompensa.oro(), partida.getOro());
        assertEquals(1, partida.getInventario().size());
    }

    private static Partida trasPrimerEncuentro() {
        return primerEncuentro(azar());
    }

    private static Partida primerEncuentro(Random azar) {
        var partida = new Partida(azar);
        partida.seleccionarHeroes(List.of(GUERRERO, ARQUERO, MAGO));
        var combate = partida.iniciarCombate();
        var enemigo = combate.getEnemigos().getFirst();
        enemigo.recibirDanio(enemigo.getPvActuales() - 1);
        partida.actuar(new AccionCombate(combate.getTurno(), partida.getEquipo().get(1).getId(), "ataque_basico", enemigo.getId()));
        return partida;
    }

    private static Objeto objeto(Partida partida, String codigo) {
        return partida.getInventario().stream().filter(objeto -> objeto.codigo().equals(codigo)).findFirst().orElseThrow();
    }

    private static Random azar(Integer... tiradas) {
        var pendientes = new ArrayDeque<>(List.of(tiradas));
        return new Random(0) {
            @Override
            public int nextInt(int limite) {
                return pendientes.isEmpty() ? limite - 1 : pendientes.removeFirst();
            }
        };
    }
}
