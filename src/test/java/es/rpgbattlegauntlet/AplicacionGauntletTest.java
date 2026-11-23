package es.rpgbattlegauntlet;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AplicacionGauntletTest {

    @LocalServerPort
    private int puerto;

    @Test
    void servidorArrancaEnUnPuertoDisponible() {
        assertTrue(puerto > 0);
    }
}
