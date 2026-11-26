package es.rpgbattlegauntlet.dto;

import es.rpgbattlegauntlet.config.CatalogoEncuentros;
import es.rpgbattlegauntlet.config.Equilibrio;
import es.rpgbattlegauntlet.enums.RanuraEquipo;
import es.rpgbattlegauntlet.enums.ClaseHeroe;
import es.rpgbattlegauntlet.enums.Dificultad;
import es.rpgbattlegauntlet.enums.EstadoPartida;
import es.rpgbattlegauntlet.enums.EstadoTemporal;
import es.rpgbattlegauntlet.model.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record VistaPartida(UUID id, long version, EstadoPartida estado, Dificultad dificultad,
                           List<Dificultad> dificultadesDisponibles, int ronda, int totalRondas, int oro, boolean oroIlimitado, List<Luchador> heroes,
                           List<Luchador> enemigos, int turno, int primerTurnoCombate, List<UUID> orden, List<Habilidad> habilidades, List<UUID> objetivosRobo,
                           List<Objeto> inventario, List<Objeto> botin, List<Oferta> tienda,
                           Recompensa recompensa, List<EventoCombate> historial,
                           List<CatalogoEncuentros.CombateDePruebas> combatesDePruebas) {

    public static VistaPartida de(Partida partida, long version) {
        Combate combate = partida.getCombate();
        return new VistaPartida(partida.getId(), version, partida.getEstado(), partida.getDificultad(),
                List.of(Dificultad.values()), partida.getRonda(), partida.getTotalRondas(), partida.getOro(), partida.tieneOroIlimitado(),
                partida.getEquipo().stream().map(Luchador::de).toList(),
                combate == null ? List.of() : combate.getEnemigos().stream().map(Luchador::de).toList(),
                combate == null ? 0 : combate.getTurno(),
                combate == null ? 0 : combate.getPrimerTurno(),
                combate == null ? List.of() : combate.getOrdenActuacion().stream().map(Combatiente::getId).toList(),
                combate == null ? List.of() : combate.getHabilidadesDelTurno(),
                combate == null ? List.of() : combate.getEnemigos().stream().filter(combate::puedeSerRobado).map(Combatiente::getId).toList(),
                partida.getInventario(), partida.getBotin(),
                partida.getEstado() == EstadoPartida.ENTRE_COMBATES || partida.getEstado() == EstadoPartida.CAMPAMENTO_INICIAL
                        ? partida.getOfertas() : List.of(),
                partida.getRecompensa(), partida.getHistorial(),
                combate == null ? CatalogoEncuentros.combatesDePruebas() : List.of());
    }

    public record Luchador(UUID id, String tipo, EstadisticasBase base, Bonificaciones modificadoresEquipo,
                           int pv, int pm, int velocidad, int defensa, int resistencia,
                           Map<EstadoTemporal, Integer> estados, Map<RanuraEquipo, Objeto> equipo, int nivel,
                           int experiencia, int experienciaSiguiente, int puntos, int probabilidadRobo) {

        static Luchador de(Combatiente combatiente) {
            Heroe heroe = combatiente instanceof Heroe jugador ? jugador : null;
            String tipo = heroe != null ? heroe.getClase().name() : ((Enemigo) combatiente).getTipo().name();
            return new Luchador(combatiente.getId(), tipo, combatiente.getEstadisticasBase(), combatiente.getBonificaciones(),
                    combatiente.getPvActuales(), combatiente.getPmActuales(), combatiente.getVelocidadEfectiva(),
                    combatiente.getDefensaEfectiva(), combatiente.getResistenciaEfectiva(), combatiente.getEstados(),
                    heroe == null ? Map.of() : heroe.getEquipo(),
                    heroe == null ? 0 : heroe.getNivel(), heroe == null ? 0 : heroe.getExperiencia(),
                    heroe == null ? 0 : Equilibrio.experienciaSiguiente(heroe.getNivel()), heroe == null ? 0 : heroe.getPuntos(),
                    heroe != null && heroe.getClase() == ClaseHeroe.PICARO ? heroe.probabilidadRobo() : 0);
        }
    }
}
