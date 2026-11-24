package tn.esprit.skillmatch.alertes.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tn.esprit.skillmatch.alertes.entity.ContratType;

import java.util.List;

/** Contenu de OfferPublished, émis par offres-service. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OfferPublishedData(
        Long offerId,
        String titre,
        List<String> competences,
        String localisation,
        ContratType contrat
) {
}
