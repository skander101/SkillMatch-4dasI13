package tn.esprit.skillmatch.alertes.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

/**
 * Appel REST vers commun-service : GET /skillmatch/auth/me.
 * Le JWT actuel contient l'email (sub) et le rôle, mais pas l'id : on récupère l'id ici.
 */
@Component
public class CommunClient {

    private final RestClient restClient;

    public CommunClient(@Value("${alertes.commun.url}") String communUrl) {
        this.restClient = RestClient.builder().baseUrl(communUrl).build();
    }

    public Long fetchUserId(String authorization) {
        try {
            UtilisateurDto user = restClient.get()
                    .uri("/skillmatch/auth/me")
                    .header(HttpHeaders.AUTHORIZATION, authorization)
                    .retrieve()
                    .body(UtilisateurDto.class);
            if (user == null || user.id() == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur introuvable");
            }
            return user.id();
        } catch (HttpClientErrorException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token JWT invalide ou expiré");
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "commun-service indisponible");
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UtilisateurDto(Long id, String nom, String email, String role) {
    }
}
