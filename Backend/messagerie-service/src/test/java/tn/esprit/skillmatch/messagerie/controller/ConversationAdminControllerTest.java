package tn.esprit.skillmatch.messagerie.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tn.esprit.skillmatch.messagerie.config.AuthInterceptor;
import tn.esprit.skillmatch.messagerie.config.AuthenticatedUser;
import tn.esprit.skillmatch.messagerie.config.CurrentUserArgumentResolver;
import tn.esprit.skillmatch.messagerie.config.WebConfig;
import tn.esprit.skillmatch.messagerie.dto.AdminConversationDetailResponse;
import tn.esprit.skillmatch.messagerie.dto.AdminConversationSummaryResponse;
import tn.esprit.skillmatch.messagerie.dto.ConversationModerationRequest;
import tn.esprit.skillmatch.messagerie.dto.MessageResponse;
import tn.esprit.skillmatch.messagerie.dto.MessagerieAdminDashboardResponse;
import tn.esprit.skillmatch.messagerie.dto.MessagerieAdminStatsResponse;
import tn.esprit.skillmatch.messagerie.dto.ParticipantSummary;
import tn.esprit.skillmatch.messagerie.exception.AccessForbiddenException;
import tn.esprit.skillmatch.messagerie.exception.ResourceNotFoundException;
import tn.esprit.skillmatch.messagerie.service.ConversationService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ConversationAdminController.class)
@Import({WebConfig.class, CurrentUserArgumentResolver.class})
class ConversationAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ConversationService conversationService;

    @MockBean
    private AuthInterceptor authInterceptor;

    private final AuthenticatedUser admin = new AuthenticatedUser(99L, "admin@test.com", "ADMIN");

    @BeforeEach
    void setUp() throws Exception {
        when(authInterceptor.preHandle(any(), any(), any())).thenReturn(true);
    }

    @Test
    @DisplayName("GET /admin/supervision — 200 OK")
    void getDashboard_returns200() throws Exception {
        AdminConversationSummaryResponse conversation = new AdminConversationSummaryResponse(
                "conv-1",
                "Equipe recrutement",
                "groupe",
                3,
                12,
                LocalDateTime.of(2026, 11, 19, 11, 0),
                "signalee",
                false,
                true,
                "Bonjour",
                "Meriem",
                2L,
                List.of(new ParticipantSummary(1L, "Meriem", "RECRUTEUR"))
        );
        MessagerieAdminDashboardResponse response = new MessagerieAdminDashboardResponse(
                new MessagerieAdminStatsResponse(128, 14, 342, 3),
                List.of(conversation)
        );
        when(conversationService.getAdminDashboard(eq("GROUPES"), eq("meriem"), any())).thenReturn(response);

        mockMvc.perform(get("/skillmatch/messagerie/admin/supervision")
                        .requestAttr(AuthInterceptor.AUTHENTICATED_USER_ATTR, admin)
                        .param("scope", "GROUPES")
                        .param("query", "meriem"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stats.conversations").value(128))
                .andExpect(jsonPath("$.stats.signalementsATraiter").value(3))
                .andExpect(jsonPath("$.conversations[0].conversation").value("Equipe recrutement"))
                .andExpect(jsonPath("$.conversations[0].statut").value("signalee"));
    }

    @Test
    @DisplayName("GET /admin/supervision/export — 200 CSV")
    void exportDashboard_returnsCsv() throws Exception {
        when(conversationService.exportAdminDashboardCsv(eq("TOUTES"), eq(""), any()))
                .thenReturn("Conversation;Type\r\n\"Equipe\";\"groupe\"\r\n");

        mockMvc.perform(get("/skillmatch/messagerie/admin/supervision/export")
                        .requestAttr(AuthInterceptor.AUTHENTICATED_USER_ATTR, admin)
                        .param("scope", "TOUTES")
                        .param("query", ""))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"messagerie-supervision.csv\""))
                .andExpect(content().contentTypeCompatibleWith(MediaType.parseMediaType("text/csv")))
                .andExpect(content().string("Conversation;Type\r\n\"Equipe\";\"groupe\"\r\n"));
    }

    @Test
    @DisplayName("PATCH /admin/conversations/{id}/moderation — 200 OK")
    void updateModeration_returns200() throws Exception {
        ConversationModerationRequest request = new ConversationModerationRequest(true, false);
        AdminConversationSummaryResponse response = new AdminConversationSummaryResponse(
                "conv-1",
                "Meriem / Yassine",
                "directe",
                2,
                24,
                LocalDateTime.of(2026, 11, 19, 10, 0),
                "signalee",
                false,
                true,
                "Merci",
                "Meriem",
                0L,
                List.of()
        );
        when(conversationService.updateModeration(eq("conv-1"), any(), any())).thenReturn(response);

        mockMvc.perform(patch("/skillmatch/messagerie/admin/conversations/conv-1/moderation")
                        .requestAttr(AuthInterceptor.AUTHENTICATED_USER_ATTR, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("conv-1"))
                .andExpect(jsonPath("$.signalee").value(true))
                .andExpect(jsonPath("$.archivee").value(false));
    }

    @Test
    @DisplayName("GET /admin/supervision — 403 si service refuse l'accès")
    void getDashboard_forbidden_returns403() throws Exception {
        when(conversationService.getAdminDashboard(isNull(), isNull(), any()))
                .thenThrow(new AccessForbiddenException("Cette fonctionnalité est réservée aux administrateurs."));

        mockMvc.perform(get("/skillmatch/messagerie/admin/supervision")
                        .requestAttr(AuthInterceptor.AUTHENTICATED_USER_ATTR, new AuthenticatedUser(1L, "user@test.com", "CANDIDAT")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("GET /admin/conversations/{id} — 200 OK avec messages")
    void getConversationDetail_returns200() throws Exception {
        AdminConversationSummaryResponse summary = new AdminConversationSummaryResponse(
                "conv-1",
                "Meriem / Yassine",
                "directe",
                2,
                1,
                LocalDateTime.of(2026, 11, 25, 11, 0),
                "active",
                false,
                false,
                "Bonjour",
                "Meriem",
                0L,
                List.of(new ParticipantSummary(1L, "Meriem", "CANDIDAT"))
        );
        MessageResponse message = new MessageResponse(
                "msg-1",
                "Bonjour",
                "texte",
                LocalDateTime.of(2026, 11, 25, 11, 0),
                true,
                "conv-1",
                1L,
                "Meriem",
                null,
                null,
                null
        );
        AdminConversationDetailResponse detail = new AdminConversationDetailResponse(
                summary,
                LocalDateTime.of(2026, 11, 24, 9, 0),
                null,
                null,
                1L,
                List.of(message)
        );
        when(conversationService.getAdminConversationDetail(eq("conv-1"), any())).thenReturn(detail);

        mockMvc.perform(get("/skillmatch/messagerie/admin/conversations/conv-1")
                        .requestAttr(AuthInterceptor.AUTHENTICATED_USER_ATTR, admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversation.id").value("conv-1"))
                .andExpect(jsonPath("$.messages[0].contenu").value("Bonjour"))
                .andExpect(jsonPath("$.messages[0].senderNom").value("Meriem"));
    }

    @Test
    @DisplayName("GET /admin/conversations/{id} — 404 si introuvable")
    void getConversationDetail_notFound_returns404() throws Exception {
        when(conversationService.getAdminConversationDetail(eq("unknown"), any()))
                .thenThrow(new ResourceNotFoundException("Conversation introuvable : unknown"));

        mockMvc.perform(get("/skillmatch/messagerie/admin/conversations/unknown")
                        .requestAttr(AuthInterceptor.AUTHENTICATED_USER_ATTR, admin))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
