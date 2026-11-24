package tn.esprit.skillmatch.messagerie.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Corps de POST /skillmatch/messagerie/conversations/{id}/messages.
 * Le senderId n'apparaît PAS ici.
 */
public record SendMessageRequest(
        @NotBlank(message = "Le contenu du message ne peut pas être vide")
        String contenu,
        String type,
        String fichierNom,
        String fichierUrl,
        Long fichierTaille
) {
    public SendMessageRequest(String contenu) {
        this(contenu, "texte", null, null, null);
    }
}
