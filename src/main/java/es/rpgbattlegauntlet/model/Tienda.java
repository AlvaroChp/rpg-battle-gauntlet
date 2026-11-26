package es.rpgbattlegauntlet.model;

import es.rpgbattlegauntlet.algorithm.GeneradorBotin;
import es.rpgbattlegauntlet.config.CatalogoObjetos;
import es.rpgbattlegauntlet.enums.Calidad;
import es.rpgbattlegauntlet.enums.Categoria;
import es.rpgbattlegauntlet.enums.TipoEnemigo;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.stream.Collectors;

import static es.rpgbattlegauntlet.enums.TipoEnemigo.*;

final class Tienda {

    private static final Set<TipoEnemigo> JEFES_DRACONICOS = Set.of(DRACONIANO_CIMITARRA, DRACONIANO_BALLESTA,
            GUIVERNO, DRAGON_ESCARCHA, DRAGON_OBSIDIANA, DIOS_DRAGON);
    private static final Set<TipoEnemigo> JEFES = Set.of(OGRO, CABALLERO_OSCURO, VERDUGO_ESPECTRAL,
            NIGROMANTE, GOLEM_RUNICO, DRAGON);
    private static final Set<String> ARMAS_DE_JEFE = Stream.concat(JEFES.stream(), JEFES_DRACONICOS.stream())
            .flatMap(Tienda::armasDeBotin).map(Tienda::claveArma).collect(Collectors.toUnmodifiableSet());
    private static final Set<String> ARMAS_DRACONICAS = JEFES_DRACONICOS.stream().flatMap(Tienda::armasDeBotin)
            .map(Tienda::claveArma).collect(Collectors.toUnmodifiableSet());

    private final Set<String> pergaminosAgotados = new HashSet<>();
    private final Map<UUID, Objeto> segundaMano = new LinkedHashMap<>();
    private final Map<String, Objeto> armasDesbloqueadas = new LinkedHashMap<>();

    List<Oferta> ofertas(boolean modoPruebas, boolean reinoDraconico, boolean pergaminoDragon) {
        var catalogoBase = reinoDraconico ? CatalogoObjetos.tiendaDraconica() : CatalogoObjetos.tienda();
        var catalogo = Stream.concat(
                catalogoBase.stream().filter(objeto -> !ARMAS_DE_JEFE.contains(claveArma(objeto))),
                armasDesbloqueadas.values().stream().filter(objeto -> !reinoDraconico || esBotinDraconico(objeto)))
                .filter(objeto -> !CatalogoObjetos.esEquipoInicial(objeto.codigo()));
        if (pergaminoDragon) catalogo = Stream.concat(catalogo, Stream.of(CatalogoObjetos.pergaminoDragon(true)));
        return Stream.concat(
                catalogo.map(objeto -> new Oferta(objeto, objeto.precioCatalogo(),
                        !modoPruebas && objeto.categoria() == Categoria.PERGAMINOS
                                ? (pergaminosAgotados.contains(objeto.codigo()) ? 0 : 1) : null)),
                segundaMano.values().stream()
                        .filter(objeto -> !ARMAS_DE_JEFE.contains(claveArma(objeto)) || armasDesbloqueadas.containsKey(claveArma(objeto)))
                        .filter(objeto -> !reinoDraconico || objeto.efecto() != null
                                || CatalogoObjetos.esEquipoDraconico(objeto) || esBotinDraconico(objeto))
                        .map(objeto -> new Oferta(objeto, objeto.precioRecompra(), 1))
        ).toList();
    }

    void desbloquearArmas(TipoEnemigo tipo) {
        if (!JEFES.contains(tipo) && !JEFES_DRACONICOS.contains(tipo)) return;
        armasDeBotin(tipo).forEach(arma -> armasDesbloqueadas.computeIfAbsent(claveArma(arma),
                clave -> CatalogoObjetos.plantillaEquipo(arma.codigo(), arma.calidad())));
    }

    private static Stream<Objeto> armasDeBotin(TipoEnemigo tipo) {
        return GeneradorBotin.consultar(tipo).objetos().stream().filter(objeto -> objeto.categoria() == Categoria.ARMAS);
    }

    private static String claveArma(Objeto objeto) {
        return objeto.codigo().equals("ballesta") && objeto.calidad() == Calidad.SUPERIOR
                ? "ballesta:draconica" : objeto.codigo();
    }

    private static boolean esBotinDraconico(Objeto objeto) {
        return ARMAS_DRACONICAS.contains(claveArma(objeto));
    }

    Objeto comprar(String codigo, UUID id, int oro, boolean modoPruebas, List<Oferta> ofertas) {
        if ((codigo == null) == (id == null)) {
            throw new IllegalArgumentException("Indica un producto del catálogo o una unidad de segunda mano");
        }
        Oferta oferta = ofertas.stream().filter(entrada -> id != null
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
        if (!CatalogoObjetos.esEquipoInicial(objeto.codigo())) segundaMano.put(objeto.id(), objeto);
    }
}
