package tn.esprit.skillmatch.offres.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

class ActuatorEndpointsTest {
    @TestConfiguration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class Sonde {}

    @Test
    void exposeSanteInformationsEtMetriquesSansExposerEnvironnement() throws Exception {
        // Charge le vrai YAML, avec un serveur HTTP, sans base ni broker externes.
        try (var contexte = new SpringApplicationBuilder(Sonde.class)
                .web(WebApplicationType.SERVLET).run(
                        "--server.port=0", "--spring.cloud.config.enabled=false",
                        "--spring.cloud.discovery.enabled=false", "--eureka.client.enabled=false",
                        "--spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration",
                        "--spring.main.banner-mode=off")) {
            int port = ((ServletWebServerApplicationContext) contexte).getWebServer().getPort();
            var client = HttpClient.newHttpClient();
            var json = new ObjectMapper();
            String base = "http://localhost:" + port + "/actuator/";

            var health = get(client, base + "health");
            assertEquals(200, health.statusCode());
            var sante = json.readTree(health.body());
            assertEquals("UP", sante.path("status").asText());
            assertTrue(sante.path("components").has("diskSpace"));
            assertFalse(sante.path("components").path("diskSpace").has("details"));
            assertEquals(200, get(client, base + "health/liveness").statusCode());
            assertEquals(200, get(client, base + "health/readiness").statusCode());

            var info = get(client, base + "info");
            assertEquals(200, info.statusCode());
            assertEquals("offres-service", json.readTree(info.body()).path("app").path("name").asText());

            assertEquals(200, get(client, base + "metrics").statusCode());
            var memoire = get(client, base + "metrics/jvm.memory.used");
            assertEquals(200, memoire.statusCode());
            assertTrue(json.readTree(memoire.body()).path("measurements").size() > 0);
            var requetes = get(client, base + "metrics/http.server.requests");
            assertEquals(200, requetes.statusCode());
            assertTrue(requetes.body().contains("offres-service"));
            assertEquals(404, get(client, base + "env").statusCode());
        }
    }

    private HttpResponse<String> get(HttpClient client, String url) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(url)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }
}
