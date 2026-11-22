package tn.esprit.skillmatch.messagerie.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Corps de POST /skillmatch/messagerie/conversations.
 * Le senderId n'apparaît PAS ici : il est extrait du JWT (AuthenticatedUser).
 */
public record CreateConversationRequest(
        @NotNull(message = "participantIds est obligatoire")
        @Size(min = 2, message = "Il faut au moins 2 participants")
        List<Long> participantIds
) {}
