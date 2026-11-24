package tn.esprit.skillmatch.messagerie.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AdminConversationDetailResponse(
        AdminConversationSummaryResponse conversation,
        LocalDateTime dateCreation,
        LocalDateTime dateArchivage,
        LocalDateTime dateSignalement,
        Long createurId,
        List<MessageResponse> messages
) {
}
