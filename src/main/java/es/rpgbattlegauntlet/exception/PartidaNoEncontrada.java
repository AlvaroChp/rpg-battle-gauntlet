package es.rpgbattlegauntlet.exception;

public final class PartidaNoEncontrada extends RuntimeException {

    public PartidaNoEncontrada() {
        super("La partida no existe");
    }
}
