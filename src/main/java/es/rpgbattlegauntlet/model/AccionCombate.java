package es.rpgbattlegauntlet.model;

import java.util.UUID;

public record AccionCombate(int turno, UUID actor, String habilidad, UUID objetivo) {
}
