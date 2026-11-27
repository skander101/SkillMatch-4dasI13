package tn.esprit.skillmatch.messagerie.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSignalementRequest(
        @Schema(description = "Motif du signalement : Langage inapproprié, Spam, Harcèlement, Autre", example = "Spam")
        @NotBlank(message = "Le motif est obligatoire")
        String motif,

        @Schema(description = "Commentaire facultatif", example = "Envoi répété de liens promotionnels")
        @Size(max = 500, message = "Le commentaire ne peut pas dépasser 500 caractères")
        String commentaire
) {
}
