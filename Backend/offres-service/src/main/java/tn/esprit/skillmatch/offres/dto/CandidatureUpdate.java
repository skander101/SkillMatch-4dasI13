package tn.esprit.skillmatch.offres.dto; // Formulaire de modification.

import jakarta.validation.constraints.*; // Validation des valeurs fournies.
import tn.esprit.skillmatch.offres.enums.CandidatureStatut; // Statuts UML.
import java.time.LocalDateTime; // Date et heure d'entretien.

public record CandidatureUpdate( // Un champ absent conserve sa valeur actuelle.
        @Size(max = 10000) String lettre, // Modifiable par le candidat et ADMIN.
        @Positive Long cvId, // Modifiable par le candidat et ADMIN.
        CandidatureStatut statut, // Traitement par le recruteur ou ADMIN.
        @Future LocalDateTime dateEntretien, // Nouvelle date d'entretien future.
        @PositiveOrZero Integer noteRecruteur, // Note facultative, sans échelle inventée.
        @Size(max = 10000) String commentaireRecruteur // Commentaire facultatif.
) {
}
