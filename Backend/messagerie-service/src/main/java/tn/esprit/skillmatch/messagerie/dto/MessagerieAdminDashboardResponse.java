package tn.esprit.skillmatch.messagerie.dto;

import java.util.List;

public record MessagerieAdminDashboardResponse(
        MessagerieAdminStatsResponse stats,
        List<AdminConversationSummaryResponse> conversations
) {
}
