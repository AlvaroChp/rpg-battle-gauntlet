package es.rpgbattlegauntlet.model;

/** Las existencias nulas representan un suministro ilimitado. */
public record Oferta(Objeto objeto, int precio, Integer existencias) {
}
