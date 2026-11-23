package es.rpgbattlegauntlet.model;

import es.rpgbattlegauntlet.config.Equilibrio;
import es.rpgbattlegauntlet.enums.Atributo;
import es.rpgbattlegauntlet.enums.Categoria;
import es.rpgbattlegauntlet.enums.ClaseHeroe;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public final class Heroe extends Combatiente {

    private final ClaseHeroe clase;
    private final Map<Categoria, Objeto> equipo = new EnumMap<>(Categoria.class);
    private int nivel = 1;
    private int experiencia;
    private int puntos;

    public Heroe(ClaseHeroe clase, EstadisticasBase estadisticasBase) {
        super(estadisticasBase);
        this.clase = Objects.requireNonNull(clase, "La clase del héroe es obligatoria");
    }

    public ClaseHeroe getClase() {
        return clase;
    }

    public Map<Categoria, Objeto> getEquipo() {
        return Map.copyOf(equipo);
    }

    public int getNivel() {
        return nivel;
    }

    public int getExperiencia() {
        return experiencia;
    }

    public int getPuntos() {
        return puntos;
    }

    @Override
    public Bonificaciones getBonificaciones() {
        return equipo.values().stream().map(Objeto::bonificaciones)
                .reduce(Bonificaciones.NINGUNA, Bonificaciones::sumar);
    }

    Objeto equipar(Objeto objeto) {
        if (!objeto.categoria().esEquipo() || !objeto.cumpleRequisitos(this)) {
            throw new IllegalArgumentException("El héroe no cumple los requisitos del equipo");
        }
        return equipo.put(objeto.categoria(), objeto);
    }

    Objeto desequipar(Categoria categoria) {
        Objeto objeto = equipo.remove(categoria);
        if (objeto == null) {
            throw new IllegalArgumentException("La ranura está vacía");
        }
        return objeto;
    }

    Habilidad habilidadArma() {
        Objeto arma = equipo.get(Categoria.ARMAS);
        return arma == null ? null : arma.habilidad();
    }

    void ganarExperiencia(int cantidad) {
        experiencia += cantidad;
        while (experiencia >= Equilibrio.experienciaSiguiente(nivel)) {
            experiencia -= Equilibrio.experienciaSiguiente(nivel++);
            puntos += Equilibrio.PUNTOS_POR_NIVEL;
            if (estaVivo()) {
                recibirCuracion(Equilibrio.PV_AL_SUBIR);
                recuperarMagia(Equilibrio.PM_AL_SUBIR);
            }
        }
    }

    void invertir(Atributo atributo, int cantidad) {
        if (atributo == null || cantidad < 1 || cantidad > puntos) {
            throw new IllegalArgumentException("La asignación de puntos no es válida");
        }
        var base = getEstadisticasBase();
        cambiarEstadisticas(new EstadisticasBase(
                base.pvMaximos() + (atributo == Atributo.PV ? cantidad * Equilibrio.PV_POR_PUNTO : 0),
                base.pmMaximos() + (atributo == Atributo.PM ? cantidad * Equilibrio.PM_POR_PUNTO : 0),
                base.fuerza() + (atributo == Atributo.FUE ? cantidad : 0),
                base.defensa() + (atributo == Atributo.DEF ? cantidad : 0),
                base.inteligencia() + (atributo == Atributo.INT ? cantidad : 0),
                base.resistencia() + (atributo == Atributo.RES ? cantidad : 0),
                base.velocidad() + (atributo == Atributo.VEL ? cantidad : 0),
                base.critico(), base.esquiva()));
        if (estaVivo()) {
            if (atributo == Atributo.PV) {
                recibirCuracion(cantidad * Equilibrio.PV_POR_PUNTO);
            }
            if (atributo == Atributo.PM) {
                recuperarMagia(cantidad * Equilibrio.PM_POR_PUNTO);
            }
        }
        puntos -= cantidad;
    }
}
