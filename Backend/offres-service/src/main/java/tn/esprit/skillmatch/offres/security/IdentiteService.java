package tn.esprit.skillmatch.offres.security; // Résolution fiable de l'utilisateur connecté.

import org.springframework.beans.factory.annotation.Value; // Lit la configuration.
import org.springframework.http.HttpStatus; // Codes HTTP.
import org.springframework.http.client.SimpleClientHttpRequestFactory; // Permet de limiter les délais HTTP.
import org.springframework.stereotype.Service; // Objet géré par Spring.
import org.springframework.web.client.RestClient; // Client HTTP Spring déjà disponible.
import org.springframework.web.client.RestClientException; // Erreur de communication.
import org.springframework.web.client.RestClientResponseException; // Réponse HTTP en erreur.
import org.springframework.web.server.ResponseStatusException; // Erreur renvoyée à notre API.
import java.util.Set; // Liste fixe des rôles autorisés.

@Service // Réutilisable par les futurs services métier.
public class IdentiteService { // Ne lit jamais un rôle envoyé par le formulaire.
    private final RestClient client; // Client vers commun-service.
    private final JwtVerifier jwt; // Vérification locale avant la résolution de l'identité.

    public IdentiteService(RestClient.Builder builder, @Value("${commun-service.url}") String url, JwtVerifier jwt) { // Injection des dépendances.
        this.jwt = jwt;
        var factory = new SimpleClientHttpRequestFactory(); // Prépare la connexion HTTP.
        factory.setConnectTimeout(3000); // Attend au maximum trois secondes pour se connecter.
        factory.setReadTimeout(3000); // Attend au maximum trois secondes pour lire la réponse.
        client = builder.baseUrl(url).requestFactory(factory).build(); // Utilise l'adresse configurée du service commun.
    } // Fin du constructeur.

    public Identite courante(String authorization) { // JWT local puis identité réelle dans commun-service.
        var claims = jwt.verifier(authorization); // Refuse immédiatement un jeton faux ou expiré.
        Identite identite; // Identité provenant exclusivement du serveur commun.
        try { // Appelle l'authentification existante.
            identite = client.get().uri("/skillmatch/auth/me").header("Authorization", authorization) // Transmet le JWT sans le modifier.
                    .retrieve().body(Identite.class); // Le service commun vérifie la signature et l'expiration.
        } catch (RestClientResponseException exception) { // Réponse en erreur du service commun.
            if (exception.getStatusCode().is4xxClientError())
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session refusée par le service commun"); // Identité refusée.
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Service d'authentification indisponible"); // Panne serveur, aucun accès accordé.
        } catch (RestClientException exception) { // Service commun inaccessible ou réponse illisible.
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Service d'authentification indisponible"); // Aucun accès accordé en cas de panne.
        } // Fin de l'appel HTTP.
        if (identite == null || identite.id() == null || identite.id() <= 0 || identite.role() == null // Vérifie les informations obligatoires.
                || !Set.of("ADMIN", "RECRUTEUR", "CANDIDAT").contains(identite.role())) { // Accepte uniquement les trois rôles du projet.
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Identité invalide"); // Ne fait pas confiance à une réponse incomplète.
        } // Fin du contrôle.
        if (!claims.getSubject().equals(identite.email()) || !claims.get("role", String.class).equals(identite.role()))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Identité ou rôle modifié : reconnectez-vous"); // Refuse un JWT devenu incohérent.
        return identite; // Retourne l'identité vérifiée côté serveur.
    } // Fin de la résolution.

    public void verifierRecruteur(Long id, String authorization) { // Contrôle le propriétaire choisi par ADMIN.
        verifierUtilisateur(id, authorization, "RECRUTEUR"); // Réutilise la vérification commune.
    }

    public void verifierCandidat(Long id, String authorization) { // Contrôle le candidat choisi par ADMIN.
        verifierUtilisateur(id, authorization, "CANDIDAT"); // Ne crée aucun utilisateur fictif.
    }

    private void verifierUtilisateur(Long id, String authorization, String role) { // Même vérification pour les deux rôles.
        if (id == null || id <= 0) { // Un administrateur doit choisir un utilisateur.
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choisir un utilisateur existant"); // Réponse 400.
        } // Fin du contrôle.
        Identite utilisateur; // Utilisateur à vérifier dans commun-service.
        try { // Utilise le CRUD ADMIN déjà existant.
            utilisateur = client.get().uri("/skillmatch/utilisateurs/{id}", id).header("Authorization", authorization) // Transmet le JWT ADMIN.
                    .retrieve().body(Identite.class); // Récupère le vrai rôle de l'utilisateur choisi.
        } catch (RestClientResponseException exception) { // Adapte l'erreur du service commun.
            if (exception.getStatusCode().value() == 404) { // Identifiant inexistant.
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Utilisateur inexistant"); // Refuse une association inventée.
            } // Fin du contrôle 404.
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Vérification de l'utilisateur impossible"); // Refuse la création en cas d'incertitude.
        } catch (RestClientException exception) { // Panne de communication.
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Service utilisateurs indisponible"); // Réponse 503.
        } // Fin de l'appel HTTP.
        if (utilisateur == null || !id.equals(utilisateur.id()) || !role.equals(utilisateur.role())) { // Vérifie l'identifiant et le rôle demandés.
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le rôle de l'utilisateur doit être " + role); // Refuse un rôle incorrect.
        } // Fin de la vérification.
    } // Fin du contrôle administratif.

    public record Identite(Long id, String nom, String email, String role) { // Même réponse que commun-service, sans mot de passe.
    } // Fin du record.
} // Fin du service d'identité.
