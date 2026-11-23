package tn.esprit.skillmatch.entreprises.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import tn.esprit.skillmatch.entreprises.entity.StatutModeration;

public record ModerationRequest(
        @Schema(description = "Nouveau statut de modération", example = "APPROUVE", allowableValues = {"APPROUVE", "REJETE"})
        @NotNull StatutModeration statut
) {
}
