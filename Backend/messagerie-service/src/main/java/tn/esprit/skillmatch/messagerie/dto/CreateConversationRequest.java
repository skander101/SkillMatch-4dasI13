package tn.esprit.skillmatch.messagerie.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Corps de POST /skillmatch/messagerie/conversations.
 * Le senderId n'apparaît PAS ici : il est extrait du JWT (AuthenticatedUser).
 * Pour une conversation directe : participantIds = [moi, autre], nomGroupe null, type DIRECT.
 * Pour un groupe : participantIds = [moi, autres...], nomGroupe obligatoire, type GROUPE.
 */
public record CreateConversationRequest(
        @Schema(description = "Identifiants des participants (au moins 2 distincts, dont l'utilisateur connecté)", example = "[1, 2]")
        @NotNull(message = "participantIds est obligatoire")
        @Size(min = 2, message = "Il faut au moins 2 participants")
        List<Long> participantIds,

        @Schema(description = "Nom du groupe (obligatoire si type=GROUPE, 3 à 50 caractères)", example = "Équipe recrutement")
        @Size(min = 3, max = 50, message = "Le nom du groupe doit contenir entre 3 et 50 caractères")
        String nomGroupe,

        @Schema(description = "Type : DIRECT ou GROUPE", example = "DIRECT")
        String type
) {
    public CreateConversationRequest(List<Long> participantIds) {
        this(participantIds, null, "DIRECT");
    }
}
