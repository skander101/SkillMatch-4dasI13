package tn.esprit.skillmatch.messagerie.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AdminConversationSummaryResponse(
        String id,
        String conversation,
        String type,
        int membres,
        long messages,
        LocalDateTime derniereActivite,
        String statut,
        boolean archivee,
        boolean signalee,
        String dernierMessageContenu,
        String dernierMessageSenderNom,
        long nonLusCount,
        List<ParticipantSummary> participants
) {
}
