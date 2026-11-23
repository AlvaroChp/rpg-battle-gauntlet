package es.rpgbattlegauntlet.model;

import es.rpgbattlegauntlet.enums.Calidad;

public record Bonificaciones(int fuerza, int defensa, int inteligencia, int resistencia, int velocidad,
                             int critico, int esquiva, int potenciaMagica) {
    public static final Bonificaciones NINGUNA = new Bonificaciones(0, 0, 0, 0, 0, 0, 0, 0);

    public Bonificaciones ajustar(Calidad calidad) {
        return new Bonificaciones(calidad.ajustar(fuerza), calidad.ajustar(defensa), calidad.ajustar(inteligencia),
                calidad.ajustar(resistencia), calidad.ajustar(velocidad), calidad.ajustar(critico),
                calidad.ajustar(esquiva), calidad.ajustar(potenciaMagica));
    }

    public Bonificaciones sumar(Bonificaciones otra) {
        return new Bonificaciones(fuerza + otra.fuerza, defensa + otra.defensa, inteligencia + otra.inteligencia,
                resistencia + otra.resistencia, velocidad + otra.velocidad, critico + otra.critico,
                esquiva + otra.esquiva, potenciaMagica + otra.potenciaMagica);
    }
}
