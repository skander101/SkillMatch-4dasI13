package tn.esprit.skillmatch.messagerie.dto;

import java.util.List;

public record MessagerieAdminDashboardResponse(
        MessagerieAdminStatsResponse stats,
        List<AdminConversationSummaryResponse> conversations,
        long totalElements,
        int totalPages,
        int page,
        int size
) {
    public MessagerieAdminDashboardResponse(
            MessagerieAdminStatsResponse stats,
            List<AdminConversationSummaryResponse> conversations
    ) {
        this(
                stats,
                conversations,
                conversations != null ? conversations.size() : 0L,
                1,
                0,
                conversations != null && !conversations.isEmpty() ? conversations.size() : 10
        );
    }
}
