package es.rpgbattlegauntlet.model;

import es.rpgbattlegauntlet.config.Equilibrio;
import es.rpgbattlegauntlet.enums.Atributo;
import es.rpgbattlegauntlet.enums.RanuraEquipo;
import es.rpgbattlegauntlet.enums.ClaseHeroe;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class Heroe extends Combatiente {

    private final ClaseHeroe clase;
    private final Map<RanuraEquipo, Objeto> equipo = new EnumMap<>(RanuraEquipo.class);
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

    public Map<RanuraEquipo, Objeto> getEquipo() {
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

    public int probabilidadRobo() {
        return Math.min(90, 20 + 2 * getEstadisticasBase().destreza());
    }

    @Override
    public int getBonoMaximoFuriaMaldita() {
        Objeto arma = equipo.get(RanuraEquipo.ARMAS);
        return arma == null ? 0 : arma.getBonoMaximoFuriaMaldita();
    }

    @Override
    public Bonificaciones getBonificaciones() {
        return equipo.values().stream().map(Objeto::bonificaciones)
                .reduce(Bonificaciones.NINGUNA, Bonificaciones::sumar);
    }

    @Override
    int reduccionAlDefenderse() {
        return equipo.containsKey(RanuraEquipo.ESCUDO) ? Equilibrio.REDUCCION_ESCUDO : super.reduccionAlDefenderse();
    }

    List<Objeto> equipar(Objeto objeto) {
        RanuraEquipo ranura = objeto.ranura();
        if (ranura == null || !objeto.cumpleRequisitos(this)) {
            throw new IllegalArgumentException("El héroe no cumple los requisitos del equipo");
        }
        if (ranura == RanuraEquipo.ESCUDO) {
            Objeto arma = equipo.get(RanuraEquipo.ARMAS);
            if (arma == null || arma.dosManos()) {
                throw new IllegalArgumentException("El escudo requiere un arma de una mano");
            }
        }

        var retirados = new ArrayList<Objeto>(2);
        Objeto anterior = equipo.put(ranura, objeto);
        if (anterior != null) {
            retirados.add(anterior);
        }
        if (objeto.dosManos()) {
            Objeto escudo = equipo.remove(RanuraEquipo.ESCUDO);
            if (escudo != null) {
                retirados.add(escudo);
            }
        }
        return retirados;
    }

    Objeto desequipar(RanuraEquipo ranura) {
        if (ranura == RanuraEquipo.ARMAS) {
            throw new IllegalArgumentException("El arma equipada solo se puede sustituir por otra compatible");
        }
        Objeto objeto = equipo.remove(ranura);
        if (objeto == null) {
            throw new IllegalArgumentException("La ranura está vacía");
        }
        return objeto;
    }

    Habilidad habilidadArma() {
        Objeto arma = equipo.get(RanuraEquipo.ARMAS);
        return arma == null ? null : arma.habilidad();
    }

    void ganarExperiencia(int cantidad) {
        experiencia += cantidad;
        while (experiencia >= Equilibrio.experienciaSiguiente(nivel)) {
            experiencia -= Equilibrio.experienciaSiguiente(nivel);
            nivel++;
            puntos += Equilibrio.puntosAlSubir(nivel);
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
                base.critico(), base.esquiva(), base.destreza() + (atributo == Atributo.DES ? cantidad : 0)));
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
