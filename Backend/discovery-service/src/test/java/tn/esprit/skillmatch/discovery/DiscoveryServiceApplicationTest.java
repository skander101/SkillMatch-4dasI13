package tn.esprit.skillmatch.discovery;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import java.net.URI;
import java.net.http.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DiscoveryServiceApplicationTest {
    @LocalServerPort int port;

    @Test
    void exposeLeRegistreEureka() throws Exception {
        var requete = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/eureka/apps"))
                .header("Accept", "application/json").build();
        var reponse = HttpClient.newHttpClient().send(requete, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, reponse.statusCode()); // Vérifie le vrai serveur HTTP Eureka.
    }
}
