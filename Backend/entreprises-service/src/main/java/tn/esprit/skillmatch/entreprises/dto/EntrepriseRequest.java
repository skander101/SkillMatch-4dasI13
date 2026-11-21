package tn.esprit.skillmatch.entreprises.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EntrepriseRequest(
        @Schema(description = "Nom de l'entreprise", example = "TechNova") @NotBlank String nom,
        @Schema(description = "Secteur d'activité", example = "Informatique") @NotBlank String secteur,
        @Schema(description = "Ville de l'entreprise", example = "Tunis") @NotBlank String ville,
        @Schema(description = "Description courte", example = "Recrutement tech") @Size(max = 1000) String description
) {
}
