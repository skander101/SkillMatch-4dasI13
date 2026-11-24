package tn.esprit.skillmatch.messagerie.dto;

public record MessagerieAdminStatsResponse(
        long conversations,
        long groupes,
        long messagesAujourdhui,
        long signalementsATraiter,
        long nouvellesConversationsSemaine,
        long nouveauxGroupesSemaine,
        Integer heurePicActivite,
        String heurePicLibelle
) {
    public MessagerieAdminStatsResponse(
            long conversations,
            long groupes,
            long messagesAujourdhui,
            long signalementsATraiter
    ) {
        this(conversations, groupes, messagesAujourdhui, signalementsATraiter, 0L, 0L, null, null);
    }
}
