
package tn.esprit.skillmatch.offres.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import java.util.Set;


// ========== SERVICE D'IDENTITÉ ==========

// Déclare cette classe comme un service géré par Spring.
@Service

// Classe qui vérifie l'identité et le rôle des utilisateurs.
public class IdentiteService {


    // ========== DÉCLARATION DES ATTRIBUTS ==========

    // Déclare le client utilisé pour communiquer avec commun-service.
    private final RestClient client;

    // Déclare l'outil qui vérifie les tokens JWT.
    private final JwtVerifier jwt;


    // ========== CONSTRUCTEUR ==========

    // Initialise les outils nécessaires pour vérifier les utilisateurs.
    public IdentiteService(RestClient.Builder builder, @Value("${commun-service.url}") String url, JwtVerifier jwt) {

        // Initialise l'outil de vérification JWT.
        this.jwt = jwt;

        // Prépare la connexion HTTP avec commun-service.
        var factory = new SimpleClientHttpRequestFactory();

        // Limite le délai de connexion à 3 secondes.
        factory.setConnectTimeout(3000);

        // Limite le délai de lecture à 3 secondes.
        factory.setReadTimeout(3000);

        // Crée le client HTTP avec l'adresse du service commun.
        client = builder.baseUrl(url).requestFactory(factory).build();
    }


    // ========== 1. RÉCUPÉRER L'UTILISATEUR CONNECTÉ ==========

    // Vérifie l'identité de l'utilisateur connecté.
    public Identite courante(String authorization) {

        // Vérifie que le JWT est valide et non expiré.
        var claims = jwt.verifier(authorization);

        // Prépare une variable pour recevoir l'identité de l'utilisateur.
        Identite identite;

        // Essaie de récupérer l'utilisateur depuis commun-service.
        try {

            // Envoie une requête GET avec le token JWT.
            identite = client.get().uri("/skillmatch/auth/me").header("Authorization", authorization)

                // Récupère les informations de l'utilisateur connecté.
                .retrieve().body(Identite.class);

            // Intercepte une réponse HTTP en erreur du service commun.
        } catch (RestClientResponseException exception) {

            // Vérifie si le service commun refuse la requête.
            if (exception.getStatusCode().is4xxClientError())

                // Retourne une erreur 401 si la session est refusée.
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session refusée par le service commun");

            // Retourne une erreur 503 si le service d'authentification échoue.
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Service d'authentification indisponible");

            // Intercepte les erreurs de communication avec commun-service.
        } catch (RestClientException exception) {

            // Retourne une erreur 503 si le service est indisponible.
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Service d'authentification indisponible");
        }


        // ========== VÉRIFICATION DE L'IDENTITÉ ==========

        // Vérifie que les informations de l'utilisateur sont valides.
        if (identite == null || identite.id() == null || identite.id() <= 0 || identite.role() == null

            // Vérifie que le rôle est ADMIN, RECRUTEUR ou CANDIDAT.
            || !Set.of("ADMIN", "RECRUTEUR", "CANDIDAT").contains(identite.role())) {

            // Refuse une identité invalide avec une erreur 401.
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Identité invalide");
        }


        // ========== VÉRIFICATION DU JWT ==========

        // Compare l'identité et le rôle du JWT avec commun-service.
        if (!claims.getSubject().equals(identite.email()) || !claims.get("role", String.class).equals(identite.role()))

            // Refuse la session si les informations ne correspondent pas.
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Identité ou rôle modifié : reconnectez-vous");

        // Retourne l'identité vérifiée de l'utilisateur connecté.
        return identite;
    }


    // ========== 2. VÉRIFIER UN RECRUTEUR ==========

    // Vérifie qu'un utilisateur choisi possède le rôle RECRUTEUR.
    public void verifierRecruteur(Long id, String authorization) {

        // Appelle la méthode commune pour vérifier le recruteur.
        verifierUtilisateur(id, authorization, "RECRUTEUR");
    }


    // ========== 3. VÉRIFIER UN CANDIDAT ==========

    // Vérifie qu'un utilisateur choisi possède le rôle CANDIDAT.
    public void verifierCandidat(Long id, String authorization) {

        // Appelle la méthode commune pour vérifier le candidat.
        verifierUtilisateur(id, authorization, "CANDIDAT");
    }


    // ========== 4. VÉRIFIER UN UTILISATEUR ==========

    // Vérifie qu'un utilisateur existe et possède le rôle demandé.
    private void verifierUtilisateur(Long id, String authorization, String role) {

        // Vérifie que l'identifiant de l'utilisateur est valide.
        if (id == null || id <= 0) {

            // Retourne une erreur 400 si aucun utilisateur valide n'est choisi.
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choisir un utilisateur existant");
        }

        // Prépare une variable pour recevoir l'utilisateur à vérifier.
        Identite utilisateur;

        // Essaie de récupérer l'utilisateur depuis commun-service.
        try {

            // Envoie une requête GET avec l'identifiant et le JWT.
            utilisateur = client.get().uri("/skillmatch/utilisateurs/{id}", id).header("Authorization", authorization)

                // Récupère les informations de l'utilisateur demandé.
                .retrieve().body(Identite.class);

            // Intercepte les erreurs HTTP du service commun.
        } catch (RestClientResponseException exception) {

            // Vérifie si l'utilisateur demandé est introuvable.
            if (exception.getStatusCode().value() == 404) {

                // Retourne une erreur 400 si l'utilisateur n'existe pas.
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Utilisateur inexistant");
            }

            // Retourne une erreur 503 si la vérification échoue.
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Vérification de l'utilisateur impossible");

            // Intercepte les erreurs de communication avec commun-service.
        } catch (RestClientException exception) {

            // Retourne une erreur 503 si commun-service est indisponible.
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Service utilisateurs indisponible");
        }


        // ========== VÉRIFICATION DU RÔLE ==========

        // Vérifie que l'utilisateur existe avec le bon identifiant et le bon rôle.
        if (utilisateur == null || !id.equals(utilisateur.id()) || !role.equals(utilisateur.role())) {

            // Retourne une erreur 400 si le rôle ne correspond pas.
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le rôle de l'utilisateur doit être " + role);
        }
    }


    // ========== 5. INFORMATIONS DE L'UTILISATEUR ==========

    // Représente les informations d'identité reçues de commun-service.
    public record Identite(Long id, String nom, String email, String role) {
    }

}
