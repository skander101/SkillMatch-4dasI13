package tn.esprit.skillmatch.entreprises.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import tn.esprit.skillmatch.entreprises.entity.StatutModeration;

import java.time.LocalDateTime;

public record AvisResponse(
        @Schema(description = "Identifiant de l'avis", example = "1") Long id,
        @Schema(description = "Identifiant de l'entreprise", example = "1") Long entrepriseId,
        @Schema(description = "Identifiant de l'utilisateur", example = "10") Long utilisateurId,
        @Schema(description = "Nom de l'utilisateur", example = "Alice Martin") String nomUtilisateur,
        @Schema(description = "Note comprise entre 1 et 5", example = "5") int note,
        @Schema(description = "Commentaire de l'avis", example = "Très bonne expérience") String commentaire,
        @Schema(description = "Statut de modération", example = "EN_ATTENTE") StatutModeration statut,
        @Schema(description = "Date de publication") LocalDateTime dateCreation
) {
}
