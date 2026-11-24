package es.rpgbattlegauntlet.model;

import es.rpgbattlegauntlet.enums.Calidad;
import es.rpgbattlegauntlet.enums.Categoria;
import es.rpgbattlegauntlet.enums.ClaseHeroe;
import es.rpgbattlegauntlet.enums.FamiliaArma;
import es.rpgbattlegauntlet.enums.PotenciaPergamino;

import java.util.List;
import java.util.UUID;

public record Objeto(UUID id, String codigo, String nombre, Categoria categoria, Calidad calidad,
                     PotenciaPergamino potencia, boolean inicial, Bonificaciones bonificaciones,
                     int fuerzaMinima, int inteligenciaMinima, int precioCatalogo,
                     int precioVenta, int precioRecompra, Habilidad habilidad, FamiliaArma familiaArma, EfectoConsumible efecto) {

    public Objeto nuevaUnidad(boolean inicial) {
        return new Objeto(UUID.randomUUID(), codigo, nombre, categoria, calidad, potencia, inicial, bonificaciones,
                fuerzaMinima, inteligenciaMinima, precioCatalogo, precioVenta, precioRecompra, habilidad, familiaArma, efecto);
    }

    public List<ClaseHeroe> getClasesCompatibles() {
        return familiaArma == null ? List.of() : familiaArma.clasesCompatibles();
    }

    public boolean cumpleRequisitos(Heroe heroe) {
        var base = heroe.getEstadisticasBase();
        return base.fuerza() >= fuerzaMinima && base.inteligencia() >= inteligenciaMinima
                && (familiaArma == null || getClasesCompatibles().contains(heroe.getClase()));
    }
}
