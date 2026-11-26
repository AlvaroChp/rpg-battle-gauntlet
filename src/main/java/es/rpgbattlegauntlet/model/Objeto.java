package es.rpgbattlegauntlet.model;

import es.rpgbattlegauntlet.config.Equilibrio;
import es.rpgbattlegauntlet.enums.Calidad;
import es.rpgbattlegauntlet.enums.Categoria;
import es.rpgbattlegauntlet.enums.ClaseHeroe;
import es.rpgbattlegauntlet.enums.FamiliaArma;
import es.rpgbattlegauntlet.enums.RanuraEquipo;

import java.util.List;
import java.util.UUID;

public record Objeto(UUID id, String codigo, String nombre, Categoria categoria, Calidad calidad,
                     boolean inicial, Bonificaciones bonificaciones,
                     int fuerzaMinima, int inteligenciaMinima, int precioCatalogo,
                     int precioVenta, int precioRecompra, Habilidad habilidad, FamiliaArma familiaArma, EfectoConsumible efecto,
                     RanuraEquipo ranura, boolean dosManos) {

    public Objeto nuevaUnidad(boolean inicial) {
        return new Objeto(UUID.randomUUID(), codigo, nombre, categoria, calidad, inicial, bonificaciones,
                fuerzaMinima, inteligenciaMinima, precioCatalogo, precioVenta, precioRecompra, habilidad, familiaArma, efecto, ranura, dosManos);
    }

    public int getBonoMaximoFuriaMaldita() {
        return "maldita".equals(codigo) ? Equilibrio.BONO_MAXIMO_FURIA_MALDITA : 0;
    }

    public int getResistenciaFuego() {
        return "armadura_obsidiana".equals(codigo) ? 70 : 0;
    }

    public boolean isInmuneEscarcha() {
        return "armadura_escarcha".equals(codigo);
    }

    public List<ClaseHeroe> getClasesCompatibles() {
        if (ranura == RanuraEquipo.ESCUDO) {
            return List.of(ClaseHeroe.GUERRERO, ClaseHeroe.PICARO);
        }
        return familiaArma == null ? List.of() : familiaArma.clasesCompatibles();
    }

    public boolean cumpleRequisitos(Heroe heroe) {
        return cumpleRequisitos(heroe, false);
    }

    boolean cumpleRequisitos(Heroe heroe, boolean ignorarAtributos) {
        var base = heroe.getEstadisticasBase();
        var clases = getClasesCompatibles();
        return (ignorarAtributos || base.fuerza() >= fuerzaMinima && base.inteligencia() >= inteligenciaMinima)
                && (clases.isEmpty() || clases.contains(heroe.getClase()));
    }
}
