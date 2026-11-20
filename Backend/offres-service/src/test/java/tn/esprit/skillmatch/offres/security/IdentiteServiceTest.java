package tn.esprit.skillmatch.offres.security; // Tests du contrat HTTP avec le service commun.

import com.sun.net.httpserver.HttpServer; // Petit serveur local réservé au test.
import org.junit.jupiter.api.AfterEach; // Ferme le serveur après chaque test.
import org.junit.jupiter.api.BeforeEach; // Prépare le serveur.
import org.junit.jupiter.api.Test; // Tests JUnit.
import org.springframework.web.client.RestClient; // Vrai client HTTP.
import org.springframework.web.server.ResponseStatusException; // Erreurs attendues.
import java.net.InetSocketAddress; // Adresse locale avec port libre.
import java.nio.charset.StandardCharsets; // Encodage UTF-8.
import java.util.concurrent.atomic.AtomicInteger; // Compte les appels.
import static org.junit.jupiter.api.Assertions.*; // Vérifications JUnit.

class IdentiteServiceTest { // Ce faux serveur ne signe pas de JWT ; il teste les échanges HTTP et le refus d'accès.
    private HttpServer serveur; // Serveur de test temporaire.
    private IdentiteService service; // Vrai service d'identité.
    private final AtomicInteger appels = new AtomicInteger(); // Nombre d'appels distants.
    private int code; // Statut HTTP simulé.
    private String reponse; // Corps JSON simulé.
    private String authorizationRecue; // En-tête réellement transmis.

    @BeforeEach // Prépare un port différent pour chaque test.
    void preparer() throws Exception { // Aucun compte réel n'est créé.
        code = 200; // Succès par défaut.
        reponse = "{\"id\":7,\"nom\":\"Test\",\"email\":\"test@example.invalid\",\"role\":\"RECRUTEUR\"}"; // Identité serveur.
        serveur = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0); // Le système choisit un port libre.
        serveur.createContext("/skillmatch", echange -> { // Répond aux routes auth et utilisateurs.
            appels.incrementAndGet(); // Compte les demandes.
            authorizationRecue = echange.getRequestHeaders().getFirst("Authorization"); // Vérifie la transmission du JWT.
            byte[] contenu = reponse.getBytes(StandardCharsets.UTF_8); // Encode le JSON.
            echange.getResponseHeaders().set("Content-Type", "application/json"); // Annonce le format JSON.
            echange.sendResponseHeaders(code, contenu.length); // Envoie le code choisi.
            echange.getResponseBody().write(contenu); // Envoie les données.
            echange.close(); // Termine la réponse.
        }); // Fin du faux serveur.
        serveur.start(); // Démarre uniquement pour ce test.
        service = new IdentiteService(RestClient.builder(), "http://127.0.0.1:" + serveur.getAddress().getPort()); // Client vers le faux serveur.
    } // Fin de la préparation.

    @AfterEach // Libère le port après le test.
    void fermer() { // Aucun serveur de test ne reste actif.
        serveur.stop(0); // Arrête le serveur temporaire.
    } // Fin du nettoyage.

    @Test // JWT transmis tel quel.
    void litLidentiteDuServeurEtTransmetLeBearer() { // Ne fait pas confiance à un rôle du formulaire.
        assertEquals(7L, service.courante("Bearer exemple").id()); // Lit l'identifiant distant.
        assertEquals("Bearer exemple", authorizationRecue); // Vérifie l'en-tête transmis.
    } // Fin du test.

    @Test // Demande anonyme.
    void refuseUnBearerAbsentSansAppelerLeServeur() { // Contrôle local minimum.
        erreur(401, () -> service.courante(null)); // En-tête absent.
        erreur(401, () -> service.courante("Bearer ")); // Jeton vide.
        erreur(401, () -> service.courante("Basic test")); // Mauvais type.
        assertEquals(0, appels.get()); // Aucun appel distant.
    } // Fin du test.

    @Test // Refus du service commun.
    void reponseAuthEnErreurNeDonneJamaisAcces() { // commun-service peut répondre 500 pour un JWT invalide.
        code = 500; // Simule ce comportement existant.
        reponse = "{}"; // Pas d'identité valide.
        erreur(401, () -> service.courante("Bearer invalide")); // Notre API refuse l'accès.
    } // Fin du test.

    @Test // Réponse incohérente.
    void roleInconnuOuIdentiteVideSontRefuses() { // Refuse de fabriquer un utilisateur.
        reponse = "{\"id\":7,\"role\":\"SUPERUSER\"}"; // Rôle non autorisé.
        erreur(401, () -> service.courante("Bearer exemple")); // Refus.
        reponse = "{}"; // Identifiant et rôle absents.
        erreur(401, () -> service.courante("Bearer exemple")); // Refus.
    } // Fin du test.

    @Test // Propriétaire ADMIN.
    void verifieUnVraiRecruteurEtRefuseUnCandidat() { // Vérifie la réponse du serveur utilisateurs.
        service.verifierRecruteur(7L, "Bearer admin"); // Rôle RECRUTEUR par défaut.
        reponse = reponse.replace("RECRUTEUR", "CANDIDAT"); // Change le rôle retourné.
        erreur(400, () -> service.verifierRecruteur(7L, "Bearer admin")); // Mauvais rôle refusé.
    } // Fin du test.

    @Test // Utilisateur inconnu.
    void recruteurInexistantEstRefuse() { // Évite les identifiants inventés.
        code = 404; // Simule un utilisateur absent.
        reponse = "{}"; // Aucun utilisateur.
        erreur(400, () -> service.verifierRecruteur(99L, "Bearer admin")); // Refus métier.
        erreur(400, () -> service.verifierRecruteur(null, "Bearer admin")); // Propriétaire obligatoire.
    } // Fin du test.

    private void erreur(int codeAttendu, Runnable operation) { // Vérification commune.
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, operation::run); // Capture l'erreur.
        assertEquals(codeAttendu, exception.getStatusCode().value()); // Vérifie le statut.
    } // Fin du helper.
} // Fin des tests d'identité.
