package tn.esprit.skillmatch.entreprises.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record EntrepriseResponse(
        @Schema(description = "Identifiant de l'entreprise", example = "1") Long id,
        @Schema(description = "Nom de l'entreprise", example = "TechNova") String nom,
        @Schema(description = "Secteur d'activité", example = "Informatique") String secteur,
        @Schema(description = "Ville de l'entreprise", example = "Tunis") String ville,
        @Schema(description = "Description courte", example = "Recrutement tech") String description,
        @Schema(description = "Moyenne des avis approuvés", example = "0.0") double noteMoyenne
) {
}
