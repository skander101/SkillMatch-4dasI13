package tn.esprit.skillmatch.messagerie.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TraiterSignalementRequest(
        @Schema(description = "Action admin : IGNORER, AVERTIR, MASQUER_MESSAGE, SUSPENDRE_ENVOI", example = "MASQUER_MESSAGE")
        @NotBlank(message = "L'action est obligatoire")
        String action,

        @Schema(description = "Note interne facultative")
        @Size(max = 500, message = "La note ne peut pas dépasser 500 caractères")
        String noteAdmin
) {
}
