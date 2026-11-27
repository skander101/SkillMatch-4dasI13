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
        List<ParticipantSummary> participants,
        long signalementsCount
) {
    public AdminConversationSummaryResponse(
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
        this(id, conversation, type, membres, messages, derniereActivite, statut, archivee, signalee,
                dernierMessageContenu, dernierMessageSenderNom, nonLusCount, participants, 0L);
    }
}
