package tn.esprit.skillmatch.messagerie.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ConversationModerationRequest(
        @Schema(description = "Passe la conversation en statut signalé ou retire le signalement", example = "true")
        Boolean signalee,

        @Schema(description = "Archive ou désarchive la conversation", example = "false")
        Boolean archivee
) {
}
