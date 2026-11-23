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
import tn.esprit.skillmatch.messagerie.dto.MessageResponse;
import tn.esprit.skillmatch.messagerie.dto.UpdateMessageRequest;
import tn.esprit.skillmatch.messagerie.exception.AccessForbiddenException;
import tn.esprit.skillmatch.messagerie.service.MessageService;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MessageController.class)
@Import({WebConfig.class, CurrentUserArgumentResolver.class})
class MessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MessageService messageService;

    @MockBean
    private AuthInterceptor authInterceptor;

    private final AuthenticatedUser user = new AuthenticatedUser(1L, "test@test.com", "CANDIDAT");

    @BeforeEach
    void setUp() throws Exception {
        when(authInterceptor.preHandle(any(), any(), any())).thenReturn(true);
    }

    @Test
    @DisplayName("GET /messages/{id} — 200 OK")
    void getById_returns200() throws Exception {
        MessageResponse response = new MessageResponse("m1", "Hello", LocalDateTime.now(), false, "conv-1", 1L);
        when(messageService.findById(eq("m1"), any())).thenReturn(response);

        mockMvc.perform(get("/skillmatch/messagerie/messages/m1")
                        .requestAttr(AuthInterceptor.AUTHENTICATED_USER_ATTR, user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("m1"));
    }

    @Test
    @DisplayName("PUT /messages/{id} — 200 OK")
    void update_returns200() throws Exception {
        UpdateMessageRequest request = new UpdateMessageRequest("Modifié");
        MessageResponse response = new MessageResponse("m1", "Modifié", LocalDateTime.now(), false, "conv-1", 1L);
        when(messageService.update(eq("m1"), any(), any())).thenReturn(response);

        mockMvc.perform(put("/skillmatch/messagerie/messages/m1")
                        .requestAttr(AuthInterceptor.AUTHENTICATED_USER_ATTR, user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenu").value("Modifié"));
    }

    @Test
    @DisplayName("PUT /messages/{id} — 403 (non expéditeur)")
    void update_nonSender_returns403() throws Exception {
        UpdateMessageRequest request = new UpdateMessageRequest("Modifié");
        when(messageService.update(eq("m1"), any(), any()))
                .thenThrow(new AccessForbiddenException("Seul l'expéditeur peut effectuer cette action"));

        mockMvc.perform(put("/skillmatch/messagerie/messages/m1")
                        .requestAttr(AuthInterceptor.AUTHENTICATED_USER_ATTR, user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("DELETE /messages/{id} — 204 No Content")
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/skillmatch/messagerie/messages/m1")
                        .requestAttr(AuthInterceptor.AUTHENTICATED_USER_ATTR, user))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /messages/{id} — 403 (non expéditeur)")
    void delete_nonSender_returns403() throws Exception {
        doThrow(new AccessForbiddenException("Seul l'expéditeur peut effectuer cette action"))
                .when(messageService).delete(eq("m1"), any());

        mockMvc.perform(delete("/skillmatch/messagerie/messages/m1")
                        .requestAttr(AuthInterceptor.AUTHENTICATED_USER_ATTR, user))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PATCH /messages/{id}/lu — 200 OK")
    void markAsRead_returns200() throws Exception {
        MessageResponse response = new MessageResponse("m1", "Hello", LocalDateTime.now(), true, "conv-1", 2L);
        when(messageService.markAsRead(eq("m1"), any())).thenReturn(response);

        mockMvc.perform(patch("/skillmatch/messagerie/messages/m1/lu")
                        .requestAttr(AuthInterceptor.AUTHENTICATED_USER_ATTR, user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lu").value(true));
    }

    @Test
    @DisplayName("PUT /messages/{id} — 400 (contenu vide)")
    void update_emptyContent_returns400() throws Exception {
        String body = "{\"contenu\":\"\"}";

        mockMvc.perform(put("/skillmatch/messagerie/messages/m1")
                        .requestAttr(AuthInterceptor.AUTHENTICATED_USER_ATTR, user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
