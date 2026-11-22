package tn.esprit.skillmatch.messagerie.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Corps de POST /skillmatch/messagerie/conversations/{id}/messages.
 * Le senderId n'apparaît PAS ici.
 */
public record SendMessageRequest(
        @NotBlank(message = "Le contenu du message ne peut pas être vide")
        String contenu
) {}
