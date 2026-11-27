package tn.esprit.skillmatch.messagerie.dto;

import java.time.LocalDateTime;

public record SignalementResponse(
        String id,
        String conversationId,
        String messageId,
        String auteurSignalementNom,
        String auteurSignalementRole,
        String auteurMessageNom,
        String auteurMessageRole,
        String motif,
        String commentaire,
        String statut,
        String actionAdmin,
        String adminNom,
        LocalDateTime dateSignalement,
        LocalDateTime dateTraitement,
        String messageContenu,
        boolean messageMasque
) {
}
