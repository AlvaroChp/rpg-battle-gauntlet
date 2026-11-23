package es.rpgbattlegauntlet.controller;

import es.rpgbattlegauntlet.config.CatalogoHeroes;
import es.rpgbattlegauntlet.dto.PeticionPartida;
import es.rpgbattlegauntlet.dto.VistaPartida;
import es.rpgbattlegauntlet.enums.ClaseHeroe;
import es.rpgbattlegauntlet.model.EstadisticasBase;
import es.rpgbattlegauntlet.model.EventoCombate;
import es.rpgbattlegauntlet.service.GestorPartidas;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/partidas")
public class ControladorPartidas {

    private final GestorPartidas gestor;

    public ControladorPartidas(GestorPartidas gestor) {
        this.gestor = gestor;
    }

    public record Perfil(ClaseHeroe clase, EstadisticasBase base) {}

    @GetMapping("/clases")
    public List<Perfil> clases() {
        return Arrays.stream(ClaseHeroe.values())
                .map(clase -> new Perfil(clase, CatalogoHeroes.crearHeroe(clase).getEstadisticasBase())).toList();
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
