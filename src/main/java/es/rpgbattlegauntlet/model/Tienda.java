package es.rpgbattlegauntlet.model;

import es.rpgbattlegauntlet.config.CatalogoObjetos;
import es.rpgbattlegauntlet.enums.Categoria;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

final class Tienda {

    private final Set<String> pergaminosAgotados = new HashSet<>();
    private final Map<UUID, Objeto> segundaMano = new LinkedHashMap<>();

    List<Oferta> ofertas(boolean modoPruebas) {
        return Stream.concat(
                CatalogoObjetos.tienda().stream().map(objeto -> new Oferta(objeto, objeto.precioCatalogo(),
                        !modoPruebas && objeto.categoria() == Categoria.PERGAMINOS
                                ? (pergaminosAgotados.contains(objeto.codigo()) ? 0 : 1) : null)),
                segundaMano.values().stream().map(objeto -> new Oferta(objeto, objeto.precioRecompra(), 1))
        ).toList();
    }

    Objeto comprar(String codigo, UUID id, int oro, boolean modoPruebas) {
        if ((codigo == null) == (id == null)) {
            throw new IllegalArgumentException("Indica un producto del catálogo o una unidad de segunda mano");
        }
        Oferta oferta = ofertas(modoPruebas).stream().filter(entrada -> id != null
                ? id.equals(entrada.objeto().id())
                : entrada.objeto().id() == null && codigo.equals(entrada.objeto().codigo()))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("El producto no existe"));
        if (Integer.valueOf(0).equals(oferta.existencias())) {
            throw new IllegalArgumentException("El pergamino del catálogo está agotado");
        }
        if (!modoPruebas && oro < oferta.precio()) {
            throw new IllegalArgumentException("Oro insuficiente");
        }

        if (id != null) {
            return segundaMano.remove(id);
        }
        if (oferta.existencias() != null) {
            pergaminosAgotados.add(codigo);
        }
        return oferta.objeto().nuevaUnidad(false);
    }

    void recibir(Objeto objeto) {
        segundaMano.put(objeto.id(), objeto);
    }
}
