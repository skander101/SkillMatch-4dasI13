package tn.esprit.skillmatch.messagerie.dto;

import java.time.LocalDateTime;

public record AuditEntryResponse(
        String id,
        String conversationId,
        String action,
        String acteurNom,
        String acteurRole,
        String details,
        LocalDateTime dateAction
) {
}
