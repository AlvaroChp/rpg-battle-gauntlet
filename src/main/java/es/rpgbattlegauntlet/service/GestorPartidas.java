package es.rpgbattlegauntlet.service;

import es.rpgbattlegauntlet.dto.PeticionPartida;
import es.rpgbattlegauntlet.dto.VistaPartida;
import es.rpgbattlegauntlet.exception.PartidaNoEncontrada;
import es.rpgbattlegauntlet.model.AccionCombate;
import es.rpgbattlegauntlet.model.Partida;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GestorPartidas {

    private static final class Sesion {
        private final Partida partida = new Partida();
        private long version;
        private boolean cerrada;
    }

    private final Map<UUID, Sesion> sesiones = new ConcurrentHashMap<>();

    public VistaPartida crear() {
        Sesion sesion = new Sesion();
        synchronized (sesion) {
            sesiones.put(sesion.partida.getId(), sesion);
            return VistaPartida.de(sesion.partida, sesion.version);
        }
    }

    public VistaPartida consultar(UUID id) {
        Sesion sesion = buscar(id);
        synchronized (sesion) {
            exigirAbierta(sesion);
            return VistaPartida.de(sesion.partida, sesion.version);
        }
    }

    public VistaPartida ejecutar(UUID id, String operacion, PeticionPartida peticion) {
        Sesion sesion = buscar(id);
        synchronized (sesion) {
            exigirAbierta(sesion);
            if (peticion.version() == null) {
                throw new IllegalArgumentException("Debes indicar la versión actual de la partida");
            }
            if (peticion.version() != sesion.version) {
                throw new IllegalStateException("La partida ha cambiado. Actualiza el estado e inténtalo de nuevo");
            }
            Partida partida = sesion.partida;
            switch (operacion) {
                case "reiniciar" -> {
                    sesion.cerrada = true;
                    sesiones.remove(id, sesion);
                    return crear();
                }
                case "seleccion" -> partida.seleccionarHeroes(peticion.clases());
                case "iniciar" -> partida.iniciarCombate();
                case "avanzar" -> partida.avanzarRonda(peticion.ronda());
                case "accion" -> partida.actuar(new AccionCombate(peticion.turno(), peticion.actor(),
                        peticion.habilidad(), peticion.objetivo()));
                case "recoger" -> partida.recoger(peticion.objeto());
                case "equipar" -> partida.equipar(peticion.actor(), peticion.objeto());
                case "desequipar" -> partida.desequipar(peticion.actor(), peticion.categoria());
                case "comprar" -> partida.comprar(peticion.codigo(), peticion.objeto());
                case "vender" -> partida.vender(peticion.objeto());
                case "consumir" -> partida.usarConsumible(peticion.turno(), peticion.actor(), peticion.objeto(), peticion.objetivo());
                case "atributos" -> partida.invertir(peticion.actor(), peticion.atributo(), peticion.cantidad());
                default -> throw new IllegalArgumentException("Operación desconocida");
            }
            sesion.version++;
            return VistaPartida.de(partida, sesion.version);
        }
    }

    private Sesion buscar(UUID id) {
        Sesion sesion = sesiones.get(id);
        if (sesion == null) {
            throw new PartidaNoEncontrada();
        }
        return sesion;
    }

    private void exigirAbierta(Sesion sesion) {
        if (sesion.cerrada) {
            throw new PartidaNoEncontrada();
        }
    }
}
