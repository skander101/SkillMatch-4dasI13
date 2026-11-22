package tn.esprit.skillmatch.messagerie.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ConversationResponse(
        String id,
        List<Long> participantIds,
        Long candidatUserId,
        LocalDateTime dernierMessageAt
) {}
