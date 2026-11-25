package tn.esprit.skillmatch.entreprises.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AvisRequest(
        @Schema(description = "Note comprise entre 1 et 5", example = "5")
        @NotNull @Min(1) @Max(5) Integer note,

        @Schema(description = "Commentaire de l'avis", example = "Très bonne expérience")
        @NotBlank @Size(max = 1000) String commentaire
) {
}
