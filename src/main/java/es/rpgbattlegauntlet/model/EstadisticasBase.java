package es.rpgbattlegauntlet.model;

/** Atributos propios del personaje. Crítico, destreza y esquiva aportan puntos porcentuales. */
public record EstadisticasBase(
        int pvMaximos,
        int pmMaximos,
        int fuerza,
        int defensa,
        int inteligencia,
        int resistencia,
        int velocidad,
        int critico,
        int esquiva,
        int destreza
) {

    public EstadisticasBase(int pvMaximos, int pmMaximos, int fuerza, int defensa, int inteligencia, int resistencia,
                            int velocidad, int critico, int esquiva) {
        this(pvMaximos, pmMaximos, fuerza, defensa, inteligencia, resistencia, velocidad, critico, esquiva, critico);
    }

    public EstadisticasBase {
        if (pvMaximos < 1 || pmMaximos < 0 || fuerza < 0 || defensa < 0
                || inteligencia < 0 || resistencia < 0 || velocidad < 1 || destreza < 0) {
            throw new IllegalArgumentException("Atributos base fuera de rango");
        }
        if (critico < 0 || critico > 100 || esquiva < 0 || esquiva > 100) {
            throw new IllegalArgumentException("Crítico y esquiva deben estar entre 0 y 100");
        }
    }
}
