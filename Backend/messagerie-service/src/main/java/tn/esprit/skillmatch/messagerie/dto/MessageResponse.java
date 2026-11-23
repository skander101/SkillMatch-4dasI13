package tn.esprit.skillmatch.messagerie.dto;

import java.time.LocalDateTime;

public record MessageResponse(
        String id,
        String contenu,
        String type,
        LocalDateTime date,
        Boolean lu,
        String conversationId,
        Long senderId,
        String senderNom,
        String fichierNom,
        String fichierUrl,
        Long fichierTaille
) {
    public MessageResponse(String id, String contenu, LocalDateTime date, Boolean lu, String conversationId, Long senderId) {
        this(id, contenu, "texte", date, lu, conversationId, senderId, null, null, null, null);
    }
}
