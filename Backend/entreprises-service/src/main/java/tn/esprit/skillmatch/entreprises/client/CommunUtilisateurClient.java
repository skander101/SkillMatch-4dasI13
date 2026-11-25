package tn.esprit.skillmatch.entreprises.client;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@Component
public class CommunUtilisateurClient implements UtilisateurClient {

    private final RestClient restClient;

    public CommunUtilisateurClient(RestClient utilisateurRestClient) {
        this.restClient = utilisateurRestClient;
    }

    @Override
    public UtilisateurInfo currentUser(String authorization) {
        try {
            UtilisateurInfo utilisateur = restClient.get()
                    .uri("/skillmatch/auth/me")
                    .header(HttpHeaders.AUTHORIZATION, authorization)
                    .retrieve()
                    .body(UtilisateurInfo.class);

            if (utilisateur == null || utilisateur.id() == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentification requise");
            }
            return utilisateur;
        } catch (HttpStatusCodeException exception) {
            if (exception.getStatusCode().is4xxClientError()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token JWT invalide ou expiré");
            }
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Service utilisateur indisponible");
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Service utilisateur indisponible");
        }
    }
}
