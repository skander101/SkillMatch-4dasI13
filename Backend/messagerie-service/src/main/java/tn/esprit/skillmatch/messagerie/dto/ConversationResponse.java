package tn.esprit.skillmatch.messagerie.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ConversationResponse(
        String id,
        List<Long> participantIds,
        Long candidatUserId,
        String nomGroupe,
        String type,
        boolean estGroupe,
        LocalDateTime dateCreation,
        LocalDateTime dernierMessageAt,
        String dernierMessageContenu,
        Long dernierMessageSenderId,
        String dernierMessageSenderNom,
        long nonLusCount,
        List<ParticipantSummary> participants
) {
    public ConversationResponse(String id, List<Long> participantIds, Long candidatUserId, LocalDateTime dernierMessageAt) {
        this(id, participantIds, candidatUserId, null, "direct", false, null, dernierMessageAt, null, null, null, 0L, List.of());
    }
}
