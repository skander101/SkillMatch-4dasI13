package tn.esprit.skillmatch.messagerie.dto;

import java.time.LocalDateTime;

public record MessageResponse(
        String id,
        String contenu,
        LocalDateTime date,
        Boolean lu,
        String conversationId,
        Long senderId
) {}
