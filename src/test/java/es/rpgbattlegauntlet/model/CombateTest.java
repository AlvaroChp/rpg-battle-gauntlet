package es.rpgbattlegauntlet.model;

import es.rpgbattlegauntlet.algorithm.CalculoCombate;
import es.rpgbattlegauntlet.config.CatalogoEnemigos;
import es.rpgbattlegauntlet.config.CatalogoHabilidades;
import es.rpgbattlegauntlet.config.CatalogoHeroes;
import es.rpgbattlegauntlet.config.CatalogoObjetos;
import es.rpgbattlegauntlet.enums.Calidad;
import es.rpgbattlegauntlet.enums.ClaseHeroe;
import es.rpgbattlegauntlet.enums.TipoEnemigo;
import es.rpgbattlegauntlet.enums.EstadoTemporal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayDeque;
import java.util.List;
import java.util.random.RandomGenerator;

import static es.rpgbattlegauntlet.enums.ClaseHeroe.*;
import static org.junit.jupiter.api.Assertions.*;

class CombateTest {

    @ParameterizedTest
    @CsvSource({"MAGO,vara_aprendiz,Golpe de vara", "SANADOR,vara_aprendiz,Golpe de vara", "PICARO,daga,Puñalada",
            "GUERRERO,daga,Puñalada", "PICARO,espada_basica,Espadazo", "GUERRERO,espada_basica,Espadazo",
            "GUERRERO,espada_pesada,Mandoble", "ARQUERO,arco_corto,Disparo", "ARQUERO,ballesta,Disparo"})
    void ataqueBasicoUsaElNombreDelArmaEnElMenuYElHistorial(ClaseHeroe clase, String codigo, String nombre) {
        var heroe = new Heroe(clase, new EstadisticasBase(150, 90, 25, 15, 25, 13, 20, 0, 0));
        heroe.equipar(CatalogoObjetos.equipo(codigo, Calidad.NORMAL, false));
        var objetivo = rival(150, 1, 0, 0, 1, 0);
        var combate = new Combate(List.of(heroe), List.of(objetivo), azar(99, 99));
        var ataque = combate.getHabilidadesDelTurno().getFirst();
        assertEquals(nombre, ataque.nombre());
        assertEquals(0, ataque.costePm());
        actuar(combate, heroe, "ataque_basico", objetivo);
        assertEquals(nombre, combate.getHistorial().getFirst().nombreHabilidad());
    }

    @Test
    void lasHabilidadesDelPicaroSePuedenMostrarAunqueNoTengaPc() {
        var picaro = CatalogoHeroes.crearHeroe(PICARO);
        picaro.equipar(CatalogoObjetos.equipo("daga", Calidad.NORMAL, true));
        picaro.consumirMagia(picaro.getPmActuales());
        var combate = new Combate(List.of(picaro), List.of(rival(100, 1, 0, 0, 1, 0)), azar());
        assertEquals(List.of("ataque_basico", "defender"),
                combate.getHabilidadesDisponibles().stream().map(Habilidad::id).toList());
        var habilidades = combate.getHabilidadesDelTurno();
        assertTrue(habilidades.stream().anyMatch(h -> h.id().equals("robar") && h.costePm() == 5));
        assertTrue(habilidades.stream().anyMatch(h -> h.id().equals("estocada_rapida") && h.costePm() == 3));
    }

    @Test
    void bendicionSeRegistraYProtegeAntesDelTurnoEnemigo() {
        var sanadora = new Heroe(SANADOR, new EstadisticasBase(100, 90, 5, 8, 15, 13, 20, 0, 0));
        var guerrero = CatalogoHeroes.crearHeroe(GUERRERO);
        guerrero.recibirDanio(10);
        var caballero = CatalogoEnemigos.crear(TipoEnemigo.CABALLERO_OSCURO);
        int danioSinBendicion = CalculoCombate.danio(caballero, guerrero, CatalogoEnemigos.ESPADA_MALDITA, false);
        var combate = new Combate(List.of(sanadora, guerrero), List.of(caballero), azar(0, 99, 99));

        actuar(combate, sanadora, "bendicion", guerrero);

        var bendicion = combate.getHistorial().get(0);
        assertEquals("bendicion", bendicion.habilidad());
        assertTrue(bendicion.cambioEstados().antes().isEmpty());
        assertEquals(2, bendicion.cambioEstados().despues().get(EstadoTemporal.BENDICION));
        var golpe = combate.getHistorial().get(1);
        assertEquals(guerrero.getId(), golpe.objetivo());
        assertEquals(Math.round(danioSinBendicion * .75), golpe.danio());
    }

    @ParameterizedTest
    @CsvSource({"100,50,99,99,36,18", "100,50,99,0,54,27", "15,50,99,99,15,7",
            "100,98,99,99,36,2", "100,50,0,99,0,0"})
    void sangriaRitualCuraSegunDanioRealSinSuperarLosPvMaximos(int pvRival, int pvPortador,
            int tiradaEsquiva, int tiradaCritico, int danio, int curacion) {
        var picaro = new Heroe(PICARO, new EstadisticasBase(100, 25, 12, 6, 4, 7, 17, 20, 15));
        picaro.equipar(CatalogoObjetos.equipo("daga_ritual", Calidad.NORMAL, false));
        picaro.recibirDanio(100 - pvPortador);
        var objetivo = rival(pvRival, 1, 6, 0, 1, 10);
        var combate = new Combate(List.of(picaro), List.of(objetivo), azar(tiradaEsquiva, tiradaCritico));

        actuar(combate, picaro, "sangria_ritual", objetivo);

        var evento = combate.getHistorial().getFirst();
        assertEquals(danio, evento.danio());
        assertEquals(curacion, evento.curacionActor());
        assertEquals(pvPortador + curacion, picaro.getPvActuales());
        assertEquals(pvRival - danio, objetivo.getPvActuales());
        assertEquals(20, picaro.getPmActuales());
    }

    @ParameterizedTest
    @CsvSource({"GUERRERO", "PICARO"})
    void furiaMalditaAumentaConLaVidaPerdidaYDesapareceAlCambiarDeArma(ClaseHeroe clase) {
        var portador = new Heroe(clase, new EstadisticasBase(100, 30, 22, 0, 10, 0, 8, 0, 0));
        var objetivo = rival(200, 1, 0, 0, 1, 0);
        var ataque = CatalogoHabilidades.ATAQUE_BASICO;
        portador.equipar(CatalogoObjetos.equipo("maldita", Calidad.NORMAL, false));

        assertEquals(72, CalculoCombate.danio(portador, objetivo, ataque, false));
        int danoHabilidadConVidaCompleta = CalculoCombate.danio(portador, objetivo, portador.habilidadArma(), false);
        portador.recibirDanio(50);
        assertEquals(83, CalculoCombate.danio(portador, objetivo, ataque, false));
        assertTrue(CalculoCombate.danio(portador, objetivo, portador.habilidadArma(), false) > danoHabilidadConVidaCompleta);
        portador.recibirDanio(49);
        assertEquals(93, CalculoCombate.danio(portador, objetivo, ataque, false));
        portador.recibirCuracion(99);
        assertEquals(72, CalculoCombate.danio(portador, objetivo, ataque, false));
        portador.recibirDanio(99);
        portador.equipar(CatalogoObjetos.equipo("espada_basica", Calidad.NORMAL, false));
        assertEquals(52, CalculoCombate.danio(portador, objetivo, ataque, false));
    }

    @Test
    void furiaDelCaballeroAfectaSuEspadaYContraataquePeroNoAlDanioMagico() {
        var caballero = new Enemigo(TipoEnemigo.CABALLERO_OSCURO,
                new EstadisticasBase(100, 30, 20, 0, 14, 0, 8, 0, 0), 2);
        var objetivo = rival(200, 1, 0, 0, 1, 0);
        var espada = CatalogoEnemigos.ESPADA_MALDITA;
        var reaccion = CatalogoEnemigos.CONTRAATAQUE;
        var hechizo = CatalogoEnemigos.BOLA_FUEGO;
        int contraataqueInicial = CalculoCombate.danio(caballero, objetivo, reaccion, false);
        int magiaInicial = CalculoCombate.danio(caballero, objetivo, hechizo, false);
        assertEquals(96, CalculoCombate.danio(caballero, objetivo, espada, false));

        caballero.recibirDanio(50);

        assertEquals(110, CalculoCombate.danio(caballero, objetivo, espada, false));
        assertTrue(CalculoCombate.danio(caballero, objetivo, reaccion, false) > contraataqueInicial);
        assertEquals(magiaInicial, CalculoCombate.danio(caballero, objetivo, hechizo, false));
    }

    @ParameterizedTest
    @CsvSource({"false,false,15", "true,false,15", "false,true,8", "true,true,8"})
    void interceptarProtegeAlGuerreroAunqueElGolpeVayaDirigidoAlMago(boolean apuntaAlMago, boolean llevaEscudo, int danio) {
        var guerrero = CatalogoHeroes.crearHeroe(GUERRERO);
        if (llevaEscudo) {
            guerrero.equipar(CatalogoObjetos.equipo("espada_basica", Calidad.NORMAL, true));
            guerrero.equipar(CatalogoObjetos.equipo("escudo", Calidad.NORMAL, false));
        }
        var mago = CatalogoHeroes.crearHeroe(MAGO);
        var enemigo = rival(100, 20, 0, 0, 1, 0);
        var combate = new Combate(List.of(guerrero, mago), List.of(enemigo), azar(99, 99));
        actuar(combate, mago, "defender", null);
        assertTrue(combate.getHabilidadesDisponibles().stream().noneMatch(habilidad -> habilidad.id().equals("guardia")));
        actuar(combate, guerrero, "interceptar", null);
        actuar(combate, enemigo, "ataque_basico", apuntaAlMago ? mago : guerrero);
        var golpe = combate.getHistorial().getLast();
        assertEquals(guerrero.getId(), golpe.objetivo());
        assertEquals(danio, golpe.danio());
        assertEquals(75, mago.getPvActuales());
        assertEquals(16, guerrero.getPmActuales());
        assertFalse(guerrero.tieneEstado(EstadoTemporal.INTERCEPTANDO));
        assertEquals(apuntaAlMago, combate.getHistorial().stream().anyMatch(e -> e.habilidad().equals("golpe_interceptado")));
    }

    @Test
    void defenderseConEscudoReduceUn65PorCientoSoloMientrasDuraLaDefensa() {
        var guerrero = CatalogoHeroes.crearHeroe(GUERRERO);
        guerrero.equipar(CatalogoObjetos.equipo("espada_basica", Calidad.NORMAL, true));
        guerrero.equipar(CatalogoObjetos.equipo("escudo", Calidad.NORMAL, false));
        var enemigo = rival(100, 20, 0, 0, 1, 0);
        var combate = new Combate(List.of(guerrero), List.of(enemigo), azar(99, 99, 99, 99));
        actuar(combate, guerrero, "defender", null);
        assertEquals(65, guerrero.getReduccionDanio());
        actuar(combate, enemigo, "ataque_basico", guerrero);
        assertEquals(8, combate.getHistorial().getLast().danio());
        actuar(combate, guerrero, "ataque_basico", enemigo);
        assertEquals(0, guerrero.getReduccionDanio());
    }

    @Test
    void ataqueFisicoDistingueEsquivaYCriticoYAplicaDefensa() {
        var guerrero = CatalogoHeroes.crearHeroe(GUERRERO);
        var rival = rival(100, 10, 40, 0, 5, 10);
        var combate = new Combate(List.of(guerrero), List.of(rival), azar(5, 10, 0));

        actuar(combate, guerrero, "ataque_basico", rival);
        actuar(combate, rival, "defender", null);
        actuar(combate, guerrero, "golpe_poderoso", rival);

        assertTrue(combate.getHistorial().getFirst().esquivado());
        assertEquals(0, combate.getHistorial().getFirst().danio());
        var golpe = combate.getHistorial().getLast();
        assertTrue(golpe.critico());
        assertEquals(10, golpe.danio());
        assertEquals(90, rival.getPvActuales());
        assertEquals(15, guerrero.getPmActuales());
    }

    @Test
    void descargaArcanaResuelveCadaEnemigoYOmiteDerrotadosEnLaCola() {
        var mago = CatalogoHeroes.crearHeroe(MAGO);
        var primero = rival(12, 1, 100, 2, 5, 0);
        var segundo = rival(100, 1, 100, 18, 5, 0);
        var combate = new Combate(List.of(mago), List.of(primero, segundo), azar(99, 99, 99, 99));

        actuar(combate, mago, "descarga_arcana", null);

        assertEquals(List.of(12, 14), combate.getHistorial().stream().map(EventoCombate::danio).toList());
        assertEquals(87, mago.getPmActuales());
        assertEquals(List.of(segundo), combate.getOrdenActuacion());
        actuar(combate, segundo, "defender", null);
        assertThrows(IllegalArgumentException.class, () -> actuar(combate, mago, "bola_fuego", primero));
        assertEquals(87, mago.getPmActuales());
    }

    @Test
    void ataqueMagicoSoloTiraElEstadoSiEsDeFuego() {
        var mago = CatalogoHeroes.crearHeroe(MAGO);
        var rival = rival(120, 1, 0, 0, 5, 100);
        var combate = new Combate(List.of(mago), List.of(rival), azar(99));
        actuar(combate, mago, "bola_fuego", rival);
        var golpe = combate.getHistorial().getLast();
        assertEquals(48, golpe.danio());
        assertFalse(golpe.esquivado());
        assertFalse(golpe.critico());
    }

    @ParameterizedTest
    @CsvSource({"NINGUNO,49,true", "NINGUNO,50,false", "DEFENSA,24,true", "DEFENSA,25,false",
            "BENDICION,24,true", "BENDICION,25,false", "PROTECCION_50,49,true"})
    void defenderseYBendicionReducenLaProbabilidadDeQuemaduraYVeneno(String proteccion, int tirada, boolean aplica) {
        var mago = CatalogoHeroes.crearHeroe(MAGO);
        var arquera = CatalogoHeroes.crearHeroe(ARQUERO);
        arquera.equipar(es.rpgbattlegauntlet.config.CatalogoObjetos.equipo("arco_goblin",
                es.rpgbattlegauntlet.enums.Calidad.NORMAL, false));
        var objetivoFuego = rival(150, 1, 0, 0, 1, 0);
        var objetivoFlecha = rival(150, 1, 0, 0, 1, 0);
        if (!proteccion.equals("NINGUNO")) {
            var estado = EstadoTemporal.valueOf(proteccion);
            objetivoFuego.aplicarEstado(estado, 1);
            objetivoFlecha.aplicarEstado(estado, 1);
        }
        var combate = new Combate(List.of(mago, arquera), List.of(objetivoFuego, objetivoFlecha), azar(99, 99, tirada, tirada));
        actuar(combate, arquera, "flecha_envenenada", objetivoFlecha);
        actuar(combate, mago, "bola_fuego", objetivoFuego);
        assertEquals(aplica, objetivoFlecha.tieneEstado(EstadoTemporal.ENVENENADO));
        assertEquals(aplica, objetivoFuego.tieneEstado(EstadoTemporal.QUEMADO));
        if (aplica) {
            int pv = objetivoFuego.getPvActuales();
            actuar(combate, objetivoFuego, "defender", null);
            assertEquals(pv - 5, objetivoFuego.getPvActuales());
            assertEquals(1, objetivoFuego.getEstados().get(EstadoTemporal.QUEMADO));
        }
    }

    @Test
    void curacionRespetaMaximoYRechazaEnemigos() {
        var sanador = CatalogoHeroes.crearHeroe(SANADOR);
        var rival = rival(100, 14, 0, 0, 12, 0);
        var combate = new Combate(List.of(sanador), List.of(rival), azar(99, 99));

        actuar(combate, rival, "ataque_basico", sanador);
        assertThrows(IllegalArgumentException.class, () -> actuar(combate, sanador, "curacion", rival));
        actuar(combate, sanador, "curacion", sanador);

        assertEquals(90, sanador.getPvActuales());
        assertEquals(20, combate.getHistorial().getLast().curacion());
        assertEquals(82, sanador.getPmActuales());
    }

    @Test
    void ralentizacionAlteraSiguienteCicloYPersisteHastaQueActuaElObjetivo() {
        var arquero = CatalogoHeroes.crearHeroe(ARQUERO);
        var rival = rival(100, 1, 10, 0, 16, 0);
        var lento = rival(100, 1, 10, 0, 2, 0);
        var combate = new Combate(List.of(arquero), List.of(rival, lento), azar(99, 99, 99, 99));

        actuar(combate, rival, "defender", null);
        actuar(combate, arquero, "flecha_perforante", rival);
        actuar(combate, lento, "defender", null);

        assertEquals(List.of(arquero, rival, lento), combate.getOrdenActuacion());
        assertEquals(12, rival.getVelocidadEfectiva());
        actuar(combate, arquero, "flecha_perforante", lento);
        assertEquals(1, lento.getVelocidadEfectiva());
        assertEquals(1, lento.getEstados().get(EstadoTemporal.RALENTIZACION));

        actuar(combate, rival, "defender", null);
        assertEquals(16, rival.getVelocidadEfectiva());
        actuar(combate, lento, "defender", null);
        assertEquals(List.of(rival, arquero, lento), combate.getOrdenActuacion());
    }

    @Test
    void rechazaAccionRepetidaHabilidadAjenaYFaltaDeMagia() {
        var heroe = new Heroe(MAGO, new EstadisticasBase(75, 9, 4, 5, 18, 14, 9, 5, 5));
        var rival = rival(100, 1, 0, 0, 9, 0);
        var combate = new Combate(List.of(heroe), List.of(rival), azar(99, 99));
        var fuego = new AccionCombate(1, heroe.getId(), "bola_fuego", rival.getId());

        assertThrows(IllegalArgumentException.class, () -> actuar(combate, rival, "defender", null));
        assertThrows(IllegalArgumentException.class, () -> actuar(combate, heroe, "curacion", heroe));
        combate.ejecutar(fuego);
        actuar(combate, rival, "defender", null);
        assertThrows(IllegalArgumentException.class, () -> combate.ejecutar(fuego));
        assertThrows(IllegalArgumentException.class, () -> actuar(combate, heroe, "bola_fuego", rival));
        assertEquals(List.of("ataque_basico", "defender"),
                combate.getHabilidadesDisponibles().stream().map(Habilidad::id).toList());
        assertEquals(2, combate.getHistorial().size());
        assertEquals(3, combate.getTurno());
    }

    private static Combatiente rival(int pv, int fuerza, int defensa, int resistencia, int velocidad, int esquiva) {
        return new Combatiente(new EstadisticasBase(pv, 0, fuerza, defensa, 0, resistencia, velocidad, 0, esquiva));
    }

    private static void actuar(Combate combate, Combatiente actor, String habilidad, Combatiente objetivo) {
        combate.ejecutar(new AccionCombate(combate.getTurno(), actor.getId(), habilidad,
                objetivo == null ? null : objetivo.getId()));
    }

    private static RandomGenerator azar(Integer... tiradas) {
        var pendientes = new ArrayDeque<>(List.of(tiradas));
        return new RandomGenerator() {
            @Override
            public int nextInt(int limite) {
                assertEquals(100, limite);
                return pendientes.removeFirst();
            }

            @Override
            public long nextLong() {
                throw new AssertionError("El combate usa tiradas porcentuales");
            }
        };
    }
}
