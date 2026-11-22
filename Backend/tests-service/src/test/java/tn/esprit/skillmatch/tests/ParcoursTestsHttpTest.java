package tn.esprit.skillmatch.tests;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("verification")
class ParcoursTestsHttpTest {

    private static final String ADMIN = "admin-verification";
    private static final String CANDIDAT = "candidat-verification";
    private static final String AUTRE_CANDIDAT = "second-candidat-verification";
    private static final String RECRUTEUR = "recruteur-verification";
    private static final long ID_CANDIDAT = 41;
    private static final long ID_AUTRE_CANDIDAT = 42;

    private static final String CATALOGUE = "/api/tests";
    private static final String RESULTATS_PERSONNELS = "/api/results/me";
    private static final String DEFINITION = """
            {
              "titre": "Introduction à Java",
              "competence": "Java",
              "dureeMinutes": 15,
              "seuilReussite": 50,
              "questions": [
                {
                  "id": "q1",
                  "enonce": "Quel mot-clé déclare une classe ?",
                  "options": ["class", "def"],
                  "indexBonneReponse": 0
                },
                {
                  "id": "q2",
                  "enonce": "Quel type représente un entier ?",
                  "options": ["String", "int"],
                  "indexBonneReponse": 1
                }
              ]
            }
            """;
    private static final String ECHEC = "{\"reponses\":{\"q1\":1,\"q2\":0}}";
    private static final String AU_SEUIL = "{\"reponses\":{\"q1\":0,\"q2\":0}}";
    private static final HttpServer COMMUN = demarrerServiceCommun();

    @Autowired
    private TestRestTemplate http;

    @DynamicPropertySource
    static void renseignerServiceCommun(DynamicPropertyRegistry properties) {
        properties.add("app.commun.url", () -> "http://127.0.0.1:" + COMMUN.getAddress().getPort());
    }

    @AfterAll
    static void arreterServiceCommun() {
        COMMUN.stop(0);
    }

    @Test
    void verifieLaCorrectionLesTroisTentativesEtLaConfidentialiteDesResultats() {
        assertThat(get(CATALOGUE, null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(get(CATALOGUE, "jeton-invalide").getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(envoyer(HttpMethod.POST, CATALOGUE, RECRUTEUR, DEFINITION).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);

        long testId = creerTest();
        String cheminTest = CATALOGUE + "/" + testId;
        String soumission = cheminTest + "/submit";

        JsonNode fiche = reponse(get(cheminTest, CANDIDAT), HttpStatus.OK);
        assertThat(fiche.path("questions").size()).isEqualTo(2);
        assertThat(fiche.path("questions").get(0).path("id").asText()).isEqualTo("q1");
        assertThat(fiche.path("questions").get(0).path("options").get(0).asText()).isEqualTo("class");
        assertThat(fiche.findValue("indexBonneReponse")).isNull();
        assertThat(get(CATALOGUE, CANDIDAT).getStatusCode()).isEqualTo(HttpStatus.OK);

        assertThat(envoyer(HttpMethod.POST, soumission, RECRUTEUR, AU_SEUIL).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(envoyer(HttpMethod.POST, soumission, CANDIDAT, "{\"reponses\":{}}").getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        verifierResultat(envoyer(HttpMethod.POST, soumission, CANDIDAT, ECHEC), ID_CANDIDAT, 1, 0, false);
        verifierResultat(envoyer(HttpMethod.POST, soumission, CANDIDAT, AU_SEUIL), ID_CANDIDAT, 2, 50, true);
        assertThat(envoyer(HttpMethod.POST, soumission, CANDIDAT, AU_SEUIL).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);

        for (int tentative = 1; tentative <= 3; tentative++) {
            verifierResultat(
                    envoyer(HttpMethod.POST, soumission, AUTRE_CANDIDAT, ECHEC),
                    ID_AUTRE_CANDIDAT, tentative, 0, false
            );
        }
        assertThat(envoyer(HttpMethod.POST, soumission, AUTRE_CANDIDAT, AU_SEUIL).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);

        JsonNode historique = reponse(get(RESULTATS_PERSONNELS, CANDIDAT), HttpStatus.OK);
        assertThat(historique.size()).isEqualTo(2);
        assertThat(historique.findValues("userId"))
                .extracting(JsonNode::asLong)
                .containsExactly(ID_CANDIDAT, ID_CANDIDAT);
        JsonNode autreHistorique = reponse(get(RESULTATS_PERSONNELS, AUTRE_CANDIDAT), HttpStatus.OK);
        assertThat(autreHistorique.size()).isEqualTo(3);
        assertThat(autreHistorique.findValues("userId"))
                .extracting(JsonNode::asLong)
                .containsExactly(ID_AUTRE_CANDIDAT, ID_AUTRE_CANDIDAT, ID_AUTRE_CANDIDAT);

        String resultatsDuTest = cheminTest + "/results";
        assertThat(get(resultatsDuTest, CANDIDAT).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(reponse(get(resultatsDuTest, ADMIN), HttpStatus.OK).size()).isEqualTo(5);

        String verification = "/api/results/check?testId=" + testId + "&userId=" + ID_CANDIDAT;
        assertThat(get(verification, CANDIDAT).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(reponse(get(verification, RECRUTEUR), HttpStatus.OK).path("reussi").asBoolean()).isTrue();
        assertThat(get("/api/results/check?testId=9223372036854775807&userId=" + ID_CANDIDAT, RECRUTEUR)
                .getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void permetALAdministrateurDeModifierEtSupprimerUnTestSansResultat() {
        String chemin = CATALOGUE + "/" + creerTest();
        String nouvelleDefinition = DEFINITION.replace("Introduction à Java", "Java express");

        assertThat(envoyer(HttpMethod.PUT, chemin, CANDIDAT, nouvelleDefinition).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        JsonNode modifie = reponse(envoyer(HttpMethod.PUT, chemin, ADMIN, nouvelleDefinition), HttpStatus.OK);
        assertThat(modifie.path("titre").asText()).isEqualTo("Java express");
        assertThat(envoyer(HttpMethod.DELETE, chemin, ADMIN, null).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(get(chemin, CANDIDAT).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private long creerTest() {
        var creation = envoyer(HttpMethod.POST, CATALOGUE, ADMIN, DEFINITION);
        long id = reponse(creation, HttpStatus.CREATED).path("id").asLong();
        assertThat(id).isPositive();
        assertThat(creation.getHeaders().getLocation()).hasToString(CATALOGUE + "/" + id);
        return id;
    }

    private static void verifierResultat(
            ResponseEntity<JsonNode> response, long candidatId, int tentative, int score, boolean reussi
    ) {
        JsonNode resultat = reponse(response, HttpStatus.OK);
        assertThat(resultat.path("userId").asLong()).isEqualTo(candidatId);
        assertThat(resultat.path("tentative").asInt()).isEqualTo(tentative);
        assertThat(resultat.path("score").asInt()).isEqualTo(score);
        assertThat(resultat.path("reussi").isBoolean()).isTrue();
        assertThat(resultat.path("reussi").asBoolean()).isEqualTo(reussi);
    }

    private static JsonNode reponse(ResponseEntity<JsonNode> response, HttpStatus code) {
        assertThat(response.getStatusCode()).isEqualTo(code);
        assertThat(response.getBody()).isNotNull();
        return response.getBody();
    }

    private ResponseEntity<JsonNode> get(String chemin, String token) {
        return envoyer(HttpMethod.GET, chemin, token, null);
    }

    private ResponseEntity<JsonNode> envoyer(HttpMethod methode, String chemin, String token, String corps) {
        var headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON, MediaType.APPLICATION_PROBLEM_JSON));
        if (corps != null) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return http.exchange(chemin, methode, new HttpEntity<>(corps, headers), JsonNode.class);
    }

    private static HttpServer demarrerServiceCommun() {
        try {
            var serveur = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            serveur.createContext("/skillmatch/auth/me", echange -> {
                try (echange) {
                    var authorization = echange.getRequestHeaders().getFirst("Authorization");
                    var compte = switch (authorization == null ? "" : authorization) {
                        case "Bearer " + ADMIN -> compte(40, "ADMIN");
                        case "Bearer " + CANDIDAT -> compte(ID_CANDIDAT, "CANDIDAT");
                        case "Bearer " + AUTRE_CANDIDAT -> compte(ID_AUTRE_CANDIDAT, "CANDIDAT");
                        case "Bearer " + RECRUTEUR -> compte(43, "RECRUTEUR");
                        default -> null;
                    };
                    if (compte == null) {
                        echange.sendResponseHeaders(401, -1);
                        return;
                    }
                    byte[] json = compte.getBytes(StandardCharsets.UTF_8);
                    echange.getResponseHeaders().set("Content-Type", "application/json");
                    echange.sendResponseHeaders(200, json.length);
                    echange.getResponseBody().write(json);
                }
            });
            serveur.start();
            return serveur;
        } catch (IOException error) {
            throw new IllegalStateException("Impossible de démarrer le simulacre HTTP de Commun.", error);
        }
    }

    private static String compte(long id, String role) {
        return "{\"id\":%d,\"role\":\"%s\"}".formatted(id, role);
    }
}
