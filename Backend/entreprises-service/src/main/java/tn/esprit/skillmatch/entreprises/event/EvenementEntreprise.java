package tn.esprit.skillmatch.entreprises.event;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import tn.esprit.skillmatch.entreprises.entity.StatutModeration;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EvenementEntreprise(
        UUID eventId,
        String type,
        @JsonProperty("date") @JsonAlias("occurredAt") Instant date,
        @JsonProperty("donnees") @JsonAlias("data") Object donnees
) {

    public static final String AVIS_MODERE = "AvisModere";
    public static final String PROFILE_UPDATED = "ProfileUpdated";

    public static EvenementEntreprise avisModere(AvisModere donnees) {
        return new EvenementEntreprise(UUID.randomUUID(), AVIS_MODERE, Instant.now(), donnees);
    }

    public static EvenementEntreprise profileUpdated(ProfileUpdated donnees) {
        return new EvenementEntreprise(UUID.randomUUID(), PROFILE_UPDATED, Instant.now(), donnees);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AvisModere(
            Long avisId,
            Long entrepriseId,
            Long utilisateurId,
            String nomUtilisateur,
            int note,
            StatutModeration statut,
            double noteMoyenne,
            LocalDateTime dateModeration
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ProfileUpdated(
            @JsonAlias("userId") Long utilisateurId,
            @JsonAlias("name") String nom,
            String titre,
            @JsonAlias({"date", "dateModification"}) Instant dateModification
    ) {
    }
}
