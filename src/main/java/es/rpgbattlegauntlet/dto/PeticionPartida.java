package es.rpgbattlegauntlet.dto;

import es.rpgbattlegauntlet.enums.Atributo;
import es.rpgbattlegauntlet.enums.Categoria;
import es.rpgbattlegauntlet.enums.ClaseHeroe;
import es.rpgbattlegauntlet.enums.Dificultad;

import java.util.List;
import java.util.UUID;

public record PeticionPartida(Long version, List<ClaseHeroe> clases, int ronda, int turno, UUID actor, String habilidad,
                             UUID objetivo, UUID objeto, String codigo, Categoria categoria,
                             Atributo atributo, int cantidad, Dificultad dificultad) {
}
