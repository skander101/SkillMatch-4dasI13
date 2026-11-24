package tn.esprit.skillmatch.offres.config;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigServerClientTest {
    @TestConfiguration(proxyBeanMethods = false) // Sonde sans base de données ni scan métier.
    static class Sonde {}

    @ParameterizedTest
    @ValueSource(ints = {200, 503})
    void utiliseLeServeurOuLeSecoursLocal(int statut) throws Exception {
        HttpServer serveur = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        serveur.createContext("/", requete -> {
            byte[] json = ("{\"name\":\"offres-service\",\"profiles\":[\"default\"],\"propertySources\":["
                    + "{\"name\":\"config-test\",\"source\":{\"info.configuration.origine\":\"config-server\"}}]}")
                    .getBytes(StandardCharsets.UTF_8);
            requete.getResponseHeaders().set("Content-Type", "application/json");
            requete.sendResponseHeaders(statut, json.length);
            requete.getResponseBody().write(json);
            requete.close();
        });
        serveur.start();
        try (var contexte = new SpringApplicationBuilder(Sonde.class).web(WebApplicationType.NONE).run(
                "--spring.config.location=classpath:/application.yml",
                "--CONFIG_SERVER_URL=http://127.0.0.1:" + serveur.getAddress().getPort(),
                "--spring.main.banner-mode=off")) {
            assertEquals(statut == 200 ? "config-server" : "local",
                    contexte.getEnvironment().getProperty("info.configuration.origine"));
        } finally {
            serveur.stop(0);
        }
    }
}
