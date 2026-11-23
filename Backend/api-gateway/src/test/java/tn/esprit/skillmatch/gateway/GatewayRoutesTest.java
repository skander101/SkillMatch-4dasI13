package tn.esprit.skillmatch.gateway; // Test HTTP du routage réel de la Gateway.

import com.sun.net.httpserver.HttpServer; // Serveurs de destination simulés.
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.io.IOException;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "eureka.client.enabled=false")
class GatewayRoutesTest { // Vérifie le transport, pas la logique métier des services.
    private static final HttpServer offres = destination("offres");
    private static final HttpServer commun = destination("commun");
    @LocalServerPort int port; // Port libre choisi pour la Gateway.

    @DynamicPropertySource
    static void configurer(DynamicPropertyRegistry properties) {
        properties.add("OFFRES_SERVICE_URL", () -> "http://127.0.0.1:" + offres.getAddress().getPort());
        properties.add("COMMUN_SERVICE_URL", () -> "http://127.0.0.1:" + commun.getAddress().getPort());
    }

    @AfterAll
    static void fermer() { // Ne laisse aucun serveur de test actif.
        offres.stop(0);
        commun.stop(0);
    }

    @ParameterizedTest
    @CsvSource({"GET,/skillmatch/offres,200,offres", "POST,/skillmatch/candidatures,201,offres",
            "PUT,/skillmatch/offres/12,200,offres", "DELETE,/skillmatch/candidatures/12,204,offres",
            "GET,/skillmatch/offres/introuvable,404,offres", "GET,/skillmatch/auth/me,200,commun",
            "GET,/skillmatch/utilisateurs,200,commun"})
    void transmetLaRequete(String methode, String chemin, int code, String service) throws Exception {
        String url = chemin + "?search=java&sort=titreDesc"; // Vérifie aussi les paramètres.
        String corps = methode.equals("POST") || methode.equals("PUT") ? "corps-test" : "";
        HttpRequest requete = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + url))
                .header("Authorization", "Bearer test").method(methode, HttpRequest.BodyPublishers.ofString(corps)).build();
        HttpResponse<String> reponse = HttpClient.newHttpClient().send(requete, HttpResponse.BodyHandlers.ofString());
        assertEquals(code, reponse.statusCode()); // La Gateway conserve le code du service.
        if (code != 204) assertEquals(service + "|" + methode + "|" + url + "|Bearer test|" + corps, reponse.body());
    }

    private static HttpServer destination(String nom) {
        try {
            HttpServer serveur = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            serveur.createContext("/", appel -> { // Renvoie les informations réellement reçues.
                int code = appel.getRequestURI().getPath().endsWith("introuvable") ? 404
                        : appel.getRequestMethod().equals("POST") ? 201 : appel.getRequestMethod().equals("DELETE") ? 204 : 200;
                byte[] corps = appel.getRequestBody().readAllBytes();
                byte[] resultat = (nom + "|" + appel.getRequestMethod() + "|" + appel.getRequestURI()
                        + "|" + appel.getRequestHeaders().getFirst("Authorization") + "|" + new String(corps, StandardCharsets.UTF_8)).getBytes(StandardCharsets.UTF_8);
                appel.sendResponseHeaders(code, code == 204 ? -1 : resultat.length);
                if (code != 204) appel.getResponseBody().write(resultat);
                appel.close();
            });
            serveur.start();
            return serveur;
        } catch (IOException exception) {
            throw new IllegalStateException("Impossible de démarrer le serveur de test", exception);
        }
    }
}
