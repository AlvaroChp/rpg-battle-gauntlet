package es.rpgbattlegauntlet.enums;

import java.util.List;

import static es.rpgbattlegauntlet.enums.ClaseHeroe.*;

public enum FamiliaArma {
    ESPADA(GUERRERO, PICARO), DAGA(GUERRERO, PICARO), GARROTE(GUERRERO, PICARO), HACHA(GUERRERO, PICARO),
    VARA(MAGO, SANADOR), ARCO(ARQUERO), BALLESTA(ARQUERO);

    private final List<ClaseHeroe> clasesCompatibles;

    FamiliaArma(ClaseHeroe... clasesCompatibles) {
        this.clasesCompatibles = List.of(clasesCompatibles);
    }

    public List<ClaseHeroe> clasesCompatibles() {
        return clasesCompatibles;
    }
}
