package es.rpgbattlegauntlet;

import com.fasterxml.jackson.databind.JsonNode;
import es.rpgbattlegauntlet.enums.Dificultad;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiPartidasTest {

    @Autowired
    private TestRestTemplate cliente;

    @Test
    void bestiarioIncluyeEstadisticasArmasHabilidadesYBotin() {
        var respuesta = cliente.getForEntity("/api/partidas/bestiario", JsonNode.class);
        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        var fichas = respuesta.getBody();
        assertEquals(16, fichas.size());
        assertTrue(fichas.valueStream().allMatch(ficha -> ficha.path("habilidades").get(0).path("costePm").asInt(-1) == 0));
        assertEquals(List.of("DRAGON", "DRACONIANO_CIMITARRA", "DRACONIANO_BALLESTA",
                        "GUIVERNO", "DRAGON_ESCARCHA", "DRAGON_OBSIDIANA", "DIOS_DRAGON"),
                fichas.valueStream().skip(9).map(ficha -> ficha.path("tipo").asText()).toList());
        var habilidadesGolem = fichas.valueStream().filter(ficha -> ficha.path("tipo").asText().equals("GOLEM_RUNICO"))
                .findFirst().orElseThrow().path("habilidades");
        assertEquals("Golpe de piedra", habilidadesGolem.get(0).path("nombre").asText());
        assertEquals(List.of("ataque_basico", "preparar_onda_runica", "onda_runica", "puno_de_piedra"),
                habilidadesGolem.valueStream().map(habilidad -> habilidad.path("id").asText()).toList());
        assertEquals(List.of("ventisca", "cetro_invierno", "armadura_escarcha"), fichas.get(13).path("botin").path("objetos")
                .valueStream().map(objeto -> objeto.path("codigo").asText()).toList());
        for (int indice : List.of(10, 11)) {
            assertTrue(fichas.get(indice).path("botin").path("garantizado").asBoolean());
            assertEquals(List.of(indice == 10 ? "cimitarra_draconica" : "ballesta"), fichas.get(indice).path("botin").path("objetos")
                    .valueStream().map(objeto -> objeto.path("codigo").asText()).toList());
        }
        assertEquals(173, fichas.get(10).path("pvPorDificultad").path("EXPERTO").asInt());
        assertEquals(153, fichas.get(11).path("pvPorDificultad").path("EXPERTO").asInt());
        var arquero = fichas.valueStream().filter(ficha -> ficha.path("tipo").asText().equals("GOBLIN_ARQUERO"))
                .findFirst().orElseThrow();
        assertEquals(55, arquero.path("base").path("pvMaximos").asInt());
        var pvArquero = arquero.path("pvPorDificultad");
        assertEquals(55, pvArquero.path("FACIL").asInt());
        assertEquals(69, pvArquero.path("DIFICIL").asInt());
        assertEquals(72, pvArquero.path("EXPERTO").asInt());
        var golem = fichas.valueStream().filter(ficha -> ficha.path("tipo").asText().equals("GOLEM_RUNICO"))
                .findFirst().orElseThrow().path("pvPorDificultad");
        assertFalse(golem.has("FACIL"));
        assertEquals(350, golem.path("DIFICIL").asInt());
        assertEquals(364, golem.path("EXPERTO").asInt());
        var diosDragon = fichas.get(15).path("pvPorDificultad");
        assertEquals(1, diosDragon.size());
        assertEquals(1203, diosDragon.path("EXPERTO").asInt());
        assertEquals("flechazo_goblin", arquero.path("habilidades").get(0).path("id").asText());
        assertEquals(40, arquero.path("botin").path("probabilidad").asInt());
        assertEquals("arco_goblin", arquero.path("botin").path("armas").get(0).path("codigo").asText());
        assertEquals("arco_goblin", arquero.path("botin").path("objetos").get(0).path("codigo").asText());
        assertEquals("daga_curva", arquero.path("botin").path("objetos").get(1).path("codigo").asText());
    }

    @Test
    void sirveLaPortadaYCreaUnaPartidaQueSePuedeReiniciar() {
        var portada = cliente.getForEntity("/", String.class);
        assertEquals(HttpStatus.OK, portada.getStatusCode());
        assertTrue(portada.getBody().contains("<title>RPG Battle Gauntlet</title>"));
        var perfiles = cliente.getForObject("/api/partidas/clases", JsonNode.class);
        assertEquals(5, perfiles.size());
        assertTrue(perfiles.valueStream().allMatch(perfil -> perfil.path("equipoInicial").valueStream()
                .anyMatch(objeto -> objeto.path("codigo").asText().equals("botas_basicas"))));
        var picaro = perfiles.valueStream().filter(perfil -> perfil.path("clase").asText().equals("PICARO")).findFirst().orElseThrow();
        assertEquals(List.of("daga", "ropa", "capucha", "botas_basicas"),
                picaro.path("equipoInicial").valueStream().map(objeto -> objeto.path("codigo").asText()).toList());
        assertEquals("verde", picaro.path("regaloInicial").path("codigo").asText());

        var alta = cliente.postForEntity("/api/partidas", null, JsonNode.class);
        assertEquals(HttpStatus.CREATED, alta.getStatusCode());
        String ruta = alta.getHeaders().getLocation().getPath();
        assertEquals("SELECCION_PERSONAJES", alta.getBody().path("estado").asText());
        assertEquals(HttpStatus.BAD_REQUEST, cliente.postForEntity(ruta + "/seleccion",
                Map.of("version", 0, "clases", List.of("MAGO", "MAGO", "ARQUERO")), JsonNode.class).getStatusCode());

        var datos = Map.of("version", 0, "clases", List.of("ARQUERO", "SANADOR", "GUERRERO"));
        var primera = CompletableFuture.supplyAsync(() -> cliente.postForEntity(ruta + "/seleccion", datos, JsonNode.class));
        var segunda = CompletableFuture.supplyAsync(() -> cliente.postForEntity(ruta + "/seleccion", datos, JsonNode.class));
        var codigos = List.of(primera.join().getStatusCode(), segunda.join().getStatusCode());
        assertTrue(codigos.containsAll(List.of(HttpStatus.OK, HttpStatus.CONFLICT)));
        assertEquals(1, cliente.getForObject(ruta, JsonNode.class).path("version").asInt());

        var campamento = cliente.postForObject(ruta + "/preparar", Map.of("version", 1), JsonNode.class);
        assertEquals("CAMPAMENTO_INICIAL", campamento.path("estado").asText());
        assertEquals(100, campamento.path("oro").asInt());
        assertTrue(campamento.path("enemigos").isEmpty());
        assertEquals(List.of("azul", "cura_grupal", "roja"),
                campamento.path("inventario").valueStream().map(objeto -> objeto.path("codigo").asText()).toList());
        var compra = cliente.postForObject(ruta + "/comprar", Map.of("version", 2, "codigo", "verde"), JsonNode.class);
        assertEquals(60, compra.path("oro").asInt());
        var inicio = cliente.postForObject(ruta + "/avanzar", Map.of("version", 3, "ronda", 1), JsonNode.class);
        assertEquals("COMBATE", inicio.path("estado").asText());
        assertEquals(60, inicio.path("oro").asInt());
        assertEquals(4, inicio.path("inventario").size());
        assertEquals("FACIL", inicio.path("dificultad").asText());
        assertEquals(10, inicio.path("totalRondas").asInt());
        assertEquals(65, inicio.path("enemigos").get(0).path("pv").asInt());
        assertEquals("GOBLIN", inicio.path("enemigos").get(0).path("tipo").asText());
        assertEquals(HttpStatus.CONFLICT, cliente.postForEntity(ruta + "/comprar",
                Map.of("version", 4, "codigo", "roja"), JsonNode.class).getStatusCode());
        assertTrue(cliente.getForObject(ruta + "/historial", JsonNode.class).isArray());

        var nueva = cliente.postForObject(ruta + "/reiniciar", Map.of("version", 4), JsonNode.class);
        assertNotEquals(alta.getBody().path("id"), nueva.path("id"));
        assertEquals("SELECCION_PERSONAJES", nueva.path("estado").asText());
        assertEquals(HttpStatus.NOT_FOUND, cliente.getForEntity(ruta, JsonNode.class).getStatusCode());
    }

    @Test
    void pruebasPermiteElegirCombateInicialYLosOtrosModosLoRechazan() {
        var alta = cliente.postForEntity("/api/partidas", null, JsonNode.class);
        String ruta = alta.getHeaders().getLocation().getPath();
        var combates = alta.getBody().path("combatesDePruebas");
        assertEquals(20, combates.size());
        assertEquals("GOBLIN", combates.get(0).path("enemigos").get(0).asText());
        assertEquals("DRAGON", combates.get(15).path("enemigos").get(0).asText());
        assertEquals(List.of("DRACONIANO_CIMITARRA", "DRACONIANO_BALLESTA"),
                combates.get(15).path("refuerzos").valueStream().map(JsonNode::asText).toList());
        assertEquals("DIOS_DRAGON", combates.get(19).path("enemigos").get(0).asText());
        cliente.postForObject(ruta + "/seleccion",
                Map.of("version", 0, "clases", List.of("GUERRERO", "MAGO", "PICARO")), JsonNode.class);
        assertEquals(HttpStatus.BAD_REQUEST, cliente.postForEntity(ruta + "/iniciar",
                Map.of("version", 1, "dificultad", "EXPERTO", "ronda", 16), JsonNode.class).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, cliente.postForEntity(ruta + "/iniciar",
                Map.of("version", 1, "dificultad", "PRUEBAS", "ronda", 21), JsonNode.class).getStatusCode());

        var partida = cliente.postForObject(ruta + "/iniciar",
                Map.of("version", 1, "dificultad", "PRUEBAS", "ronda", 16), JsonNode.class);
        assertEquals("COMBATE", partida.path("estado").asText());
        assertEquals(16, partida.path("ronda").asInt());
        assertEquals(20, partida.path("totalRondas").asInt());
        assertTrue(partida.path("oroIlimitado").asBoolean());
        assertTrue(partida.path("combatesDePruebas").isEmpty());
        assertEquals(1, partida.path("enemigos").size());
        assertEquals("DRAGON", partida.path("enemigos").get(0).path("tipo").asText());
        assertEquals(1, partida.path("enemigos").get(0).path("pv").asInt());
    }

    @Test
    void iniciarEnExpertoAplicaLaDificultadYLaConservaAlConsultar() {
        var alta = cliente.postForEntity("/api/partidas", null, JsonNode.class);
        String ruta = alta.getHeaders().getLocation().getPath();
        cliente.postForObject(ruta + "/seleccion",
                Map.of("version", 0, "clases", List.of("ARQUERO", "MAGO", "PICARO")), JsonNode.class);

        var inicio = cliente.postForObject(ruta + "/iniciar", Map.of("version", 1, "dificultad", "EXPERTO"), JsonNode.class);
        assertEquals("COMBATE", inicio.path("estado").asText());
        assertEquals(Dificultad.EXPERTO.vidaEnemiga(65), inicio.path("enemigos").get(0).path("pv").asInt());
        assertEquals(15, inicio.path("totalRondas").asInt());
        var consulta = cliente.getForObject(ruta, JsonNode.class);
        assertEquals("EXPERTO", consulta.path("dificultad").asText());
        assertEquals(15, consulta.path("totalRondas").asInt());
    }
}
