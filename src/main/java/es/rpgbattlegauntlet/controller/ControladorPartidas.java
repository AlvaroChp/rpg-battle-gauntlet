package es.rpgbattlegauntlet.controller;

import es.rpgbattlegauntlet.config.CatalogoHeroes;
import es.rpgbattlegauntlet.config.CatalogoEnemigos;
import es.rpgbattlegauntlet.config.CatalogoEncuentros;
import es.rpgbattlegauntlet.config.CatalogoObjetos;
import es.rpgbattlegauntlet.algorithm.GeneradorBotin;
import es.rpgbattlegauntlet.algorithm.InteligenciaEnemiga;
import es.rpgbattlegauntlet.dto.PeticionPartida;
import es.rpgbattlegauntlet.dto.VistaPartida;
import es.rpgbattlegauntlet.enums.ClaseHeroe;
import es.rpgbattlegauntlet.enums.Dificultad;
import es.rpgbattlegauntlet.enums.TipoEnemigo;
import es.rpgbattlegauntlet.model.EstadisticasBase;
import es.rpgbattlegauntlet.model.EventoCombate;
import es.rpgbattlegauntlet.model.Habilidad;
import es.rpgbattlegauntlet.model.Objeto;
import es.rpgbattlegauntlet.service.GestorPartidas;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/partidas")
public class ControladorPartidas {

    private final GestorPartidas gestor;

    public ControladorPartidas(GestorPartidas gestor) {
        this.gestor = gestor;
    }

    public record Perfil(ClaseHeroe clase, EstadisticasBase base, List<Objeto> equipoInicial, Objeto regaloInicial) {}
    public record FichaEnemigo(TipoEnemigo tipo, EstadisticasBase base, Map<Dificultad, Integer> pvPorDificultad, List<Habilidad> habilidades,
                               GeneradorBotin.BotinPosible botin) {}

    @GetMapping("/bestiario")
    public List<FichaEnemigo> bestiario() {
        return Arrays.stream(TipoEnemigo.values()).map(tipo -> {
            var enemigo = CatalogoEnemigos.crear(tipo);
            var base = enemigo.getEstadisticasBase();
            var pv = new EnumMap<Dificultad, Integer>(Dificultad.class);
            for (var dificultad : List.of(Dificultad.FACIL, Dificultad.DIFICIL, Dificultad.EXPERTO)) {
                if (CatalogoEncuentros.apareceEn(tipo, dificultad)) {
                    pv.put(dificultad, dificultad.vidaEnemiga(base.pvMaximos()));
                }
            }
            return new FichaEnemigo(tipo, base, pv, InteligenciaEnemiga.habilidadesBestiario(enemigo), GeneradorBotin.consultar(tipo));
        }).toList();
    }

    @GetMapping("/clases")
    public List<Perfil> clases() {
        return Arrays.stream(ClaseHeroe.values())
                .map(clase -> new Perfil(clase, CatalogoHeroes.crearHeroe(clase).getEstadisticasBase(),
                        CatalogoObjetos.inicial(clase), CatalogoObjetos.regaloInicial(clase))).toList();
    }

    @PostMapping
    public ResponseEntity<VistaPartida> crear() {
        VistaPartida partida = gestor.crear();
        return ResponseEntity.created(URI.create("/api/partidas/" + partida.id())).body(partida);
    }

    @GetMapping("/{id}")
    public VistaPartida consultar(@PathVariable UUID id) {
        return gestor.consultar(id);
    }

    @GetMapping("/{id}/historial")
    public List<EventoCombate> historial(@PathVariable UUID id) {
        return gestor.consultar(id).historial();
    }

    @PostMapping("/{id}/{operacion}")
    public VistaPartida ejecutar(@PathVariable UUID id, @PathVariable String operacion, @RequestBody PeticionPartida peticion) {
        return gestor.ejecutar(id, operacion, peticion);
    }
}
