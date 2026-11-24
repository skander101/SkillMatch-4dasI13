package tn.esprit.skillmatch.alertes.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

/**
 * Appel REST synchrone vers offres-service : GET /skillmatch/offers/{id}.
 * Règle : seule une offre PUBLIEE peut être sauvegardée.
 */
@Component
public class OffresClient {

    private final RestClient restClient;
    private final boolean verificationEnabled;

    public OffresClient(@Value("${alertes.offres.url}") String offresUrl,
                        @Value("${alertes.offres.verification-enabled}") boolean verificationEnabled) {
        this.restClient = RestClient.builder().baseUrl(offresUrl).build();
        this.verificationEnabled = verificationEnabled;
    }

    /** 404 si l'offre n'existe pas, 422 si elle n'est pas publiée, 503 si offres-service ne répond pas. */
    public void verifierOffrePubliee(Long offerId) {
        if (!verificationEnabled) {
            return; // offres-service pas encore disponible
        }
        OffreDto offre;
        try {
            offre = restClient.get()
                    .uri("/skillmatch/offers/{id}", offerId)
                    .retrieve()
                    .body(OffreDto.class);
        } catch (HttpClientErrorException.NotFound exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Offre introuvable");
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "offres-service indisponible");
        }
        if (offre == null || !"PUBLIEE".equalsIgnoreCase(offre.statut())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Seule une offre publiée peut être sauvegardée");
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OffreDto(Long id, String titre, String statut) {
    }
}
