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
    void sirveLaPortadaYCreaUnaPartidaQueSePuedeReiniciar() {
        var portada = cliente.getForEntity("/", String.class);
        assertEquals(HttpStatus.OK, portada.getStatusCode());
        assertTrue(portada.getBody().contains("<title>RPG Battle Gauntlet</title>"));
        assertEquals(5, cliente.getForObject("/api/partidas/clases", JsonNode.class).size());

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

        var inicio = cliente.postForObject(ruta + "/iniciar", Map.of("version", 1), JsonNode.class);
        assertEquals("COMBATE", inicio.path("estado").asText());
        assertEquals("FACIL", inicio.path("dificultad").asText());
        assertEquals(65, inicio.path("enemigos").get(0).path("pv").asInt());
        assertEquals("GOBLIN", inicio.path("enemigos").get(0).path("tipo").asText());
        assertEquals(HttpStatus.CONFLICT, cliente.postForEntity(ruta + "/comprar",
                Map.of("version", 2, "codigo", "roja"), JsonNode.class).getStatusCode());
        assertTrue(cliente.getForObject(ruta + "/historial", JsonNode.class).isArray());

        var nueva = cliente.postForObject(ruta + "/reiniciar", Map.of("version", 2), JsonNode.class);
        assertNotEquals(alta.getBody().path("id"), nueva.path("id"));
        assertEquals("SELECCION_PERSONAJES", nueva.path("estado").asText());
        assertEquals(HttpStatus.NOT_FOUND, cliente.getForEntity(ruta, JsonNode.class).getStatusCode());
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
        assertEquals("EXPERTO", cliente.getForObject(ruta, JsonNode.class).path("dificultad").asText());
    }
}
