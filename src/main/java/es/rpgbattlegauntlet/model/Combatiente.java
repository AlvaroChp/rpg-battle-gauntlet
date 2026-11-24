package es.rpgbattlegauntlet.model;

import es.rpgbattlegauntlet.enums.EstadoTemporal;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class Combatiente {

    private final UUID id = UUID.randomUUID();
    private EstadisticasBase estadisticasBase;
    private final Map<EstadoTemporal, Integer> estados = new EnumMap<>(EstadoTemporal.class);
    private int pvActuales;
    private int pmActuales;

    public Combatiente(EstadisticasBase estadisticasBase) {
        this.estadisticasBase = Objects.requireNonNull(estadisticasBase, "Las estadísticas base son obligatorias");
        this.pvActuales = estadisticasBase.pvMaximos();
        this.pmActuales = estadisticasBase.pmMaximos();
    }

    public UUID getId() {
        return id;
    }

    public EstadisticasBase getEstadisticasBase() {
        return estadisticasBase;
    }

    public Bonificaciones getBonificaciones() {
        return Bonificaciones.NINGUNA;
    }

    void cambiarEstadisticas(EstadisticasBase nuevas) {
        estadisticasBase = nuevas;
    }

    public int getPvActuales() {
        return pvActuales;
    }

    public int getPmActuales() {
        return pmActuales;
    }

    public boolean estaVivo() {
        return pvActuales > 0;
    }

    public Map<EstadoTemporal, Integer> getEstados() {
        return Map.copyOf(estados);
    }

    public boolean tieneEstado(EstadoTemporal estado) {
        return estados.containsKey(estado);
    }

    public int getVelocidadEfectiva() {
        int cambio = estados.keySet().stream().mapToInt(EstadoTemporal::cambioVelocidad).sum();
        return Math.max(1, estadisticasBase.velocidad() + getBonificaciones().velocidad() + cambio);
    }

    public int getEsquivaEfectiva() {
        int cambio = estados.keySet().stream().mapToInt(EstadoTemporal::esquivaAdicional).sum();
        return Math.min(100, estadisticasBase.esquiva() + getBonificaciones().esquiva() + cambio);
    }

    public int getDefensaEfectiva() {
        int penalizacion = estados.containsKey(EstadoTemporal.DEFENSA_DEBILITADA) ? 5 : 0;
        return Math.max(0, estadisticasBase.defensa() + getBonificaciones().defensa() - penalizacion);
    }

    boolean retirarEstado(EstadoTemporal estado) {
        return estados.remove(estado) != null;
    }

    public int getReduccionDanio() {
        return estados.keySet().stream().mapToInt(estado -> estado == EstadoTemporal.DEFENSA
                ? reduccionAlDefenderse() : estado.reduccionDanio()).max().orElse(0);
    }

    int reduccionAlDefenderse() {
        return EstadoTemporal.DEFENSA.reduccionDanio();
    }

    void recibirDanio(int cantidad) {
        pvActuales = Math.max(0, pvActuales - cantidad);
    }

    int recibirCuracion(int cantidad) {
        int recuperados = Math.min(cantidad, estadisticasBase.pvMaximos() - pvActuales);
        pvActuales += recuperados;
        return recuperados;
    }

    int revivir(int porcentaje) {
        if (estaVivo()) {
            throw new IllegalArgumentException("La poción de revitalización requiere un héroe caído");
        }
        limpiarEstados();
        return recibirCuracion(Math.max(1, estadisticasBase.pvMaximos() * porcentaje / 100));
    }

    int recuperarMagia(int cantidad) {
        int recuperados = Math.min(cantidad, estadisticasBase.pmMaximos() - pmActuales);
        pmActuales += recuperados;
        return recuperados;
    }

    public int getResistenciaEfectiva() {
        return estadisticasBase.resistencia() + getBonificaciones().resistencia();
    }

    void consumirMagia(int coste) {
        pmActuales -= coste;
    }

    void aplicarEstado(EstadoTemporal estado, int turnos) {
        estados.merge(estado, turnos, Math::max);
    }

    List<EstadoTemporal> terminarTurno() {
        var danios = estados.keySet().stream().filter(estado -> estado.danioPeriodico() > 0).toList();
        estados.replaceAll((estado, restantes) -> restantes - 1);
        estados.values().removeIf(restantes -> restantes == 0);
        return danios;
    }

    void limpiarEstados() {
        estados.clear();
    }
}
