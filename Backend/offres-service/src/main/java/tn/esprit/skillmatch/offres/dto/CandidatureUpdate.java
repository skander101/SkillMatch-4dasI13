package tn.esprit.skillmatch.offres.dto; // Formulaire de modification.

import jakarta.validation.constraints.*; // Validation des valeurs fournies.
import tn.esprit.skillmatch.offres.enums.CandidatureStatut; // Statuts UML.
import java.time.LocalDate;
import java.time.LocalDateTime; // Date et heure d'entretien.

public record CandidatureUpdate( // Un champ absent conserve sa valeur actuelle.
        @Pattern(regexp = "(?s).*\\S.*") @Size(min = 20, max = 10000) String lettre, // Modifiable par le candidat et ADMIN.
        @Positive Long cvId, // Modifiable par le candidat et ADMIN.
        CandidatureStatut statut, // Traitement par le recruteur ou ADMIN.
        LocalDateTime dateEntretien, // Date actuelle ou future, vérifiée ci-dessous.
        @PositiveOrZero Integer noteRecruteur, // Obligatoire pour le traitement, vérifiée par le service.
        @Pattern(regexp = "(?s).*\\S.*") @Size(min = 5, max = 10000) String commentaireRecruteur // Obligatoire pour le traitement, vérifié par le service.
) {
    public CandidatureUpdate { // Un champ absent est conservé ; les textes fournis sont nettoyés.
        lettre = lettre == null ? null : lettre.trim();
        commentaireRecruteur = commentaireRecruteur == null ? null : commentaireRecruteur.trim();
    }

    @AssertTrue(message = "La date d'entretien doit être aujourd'hui ou dans le futur")
    public boolean isDateEntretienValide() {
        return dateEntretien == null || !dateEntretien.toLocalDate().isBefore(LocalDate.now());
    }
}
