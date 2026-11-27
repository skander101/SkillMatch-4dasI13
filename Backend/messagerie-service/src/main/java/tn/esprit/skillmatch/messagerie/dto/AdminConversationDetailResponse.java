package tn.esprit.skillmatch.messagerie.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AdminConversationDetailResponse(
        AdminConversationSummaryResponse conversation,
        LocalDateTime dateCreation,
        LocalDateTime dateArchivage,
        LocalDateTime dateSignalement,
        Long createurId,
        List<MessageResponse> messages,
        String createurNom,
        long totalMessages,
        long nonLusCount,
        List<SignalementResponse> signalements,
        List<AuditEntryResponse> auditLog
) {
    public AdminConversationDetailResponse(
            AdminConversationSummaryResponse conversation,
            LocalDateTime dateCreation,
            LocalDateTime dateArchivage,
            LocalDateTime dateSignalement,
            Long createurId,
            List<MessageResponse> messages
    ) {
        this(
                conversation,
                dateCreation,
                dateArchivage,
                dateSignalement,
                createurId,
                messages,
                null,
                messages != null ? messages.size() : 0L,
                conversation != null ? conversation.nonLusCount() : 0L,
                List.of(),
                List.of()
        );
    }
}
