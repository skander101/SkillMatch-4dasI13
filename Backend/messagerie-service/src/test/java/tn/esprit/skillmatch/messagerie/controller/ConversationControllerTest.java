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
import tn.esprit.skillmatch.messagerie.dto.ConversationResponse;
import tn.esprit.skillmatch.messagerie.dto.CreateConversationRequest;
import tn.esprit.skillmatch.messagerie.dto.MessageResponse;
import tn.esprit.skillmatch.messagerie.dto.SendMessageRequest;
import tn.esprit.skillmatch.messagerie.exception.ConflictException;
import tn.esprit.skillmatch.messagerie.service.ConversationService;
import tn.esprit.skillmatch.messagerie.service.MessageService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ConversationController.class)
@Import({WebConfig.class, CurrentUserArgumentResolver.class})
class ConversationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ConversationService conversationService;

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
    @DisplayName("POST /conversations — 201 Created")
    void create_returns201() throws Exception {
        CreateConversationRequest request = new CreateConversationRequest(List.of(1L, 2L));
        ConversationResponse response = new ConversationResponse("conv-1", List.of(1L, 2L), 1L, null);
        when(conversationService.create(any(), any())).thenReturn(response);

        mockMvc.perform(post("/skillmatch/messagerie/conversations")
                        .requestAttr(AuthInterceptor.AUTHENTICATED_USER_ATTR, user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("conv-1"))
                .andExpect(jsonPath("$.participantIds[0]").value(1))
                .andExpect(jsonPath("$.participantIds[1]").value(2));
    }

    @Test
    @DisplayName("POST /conversations — 409 Conflict (doublon)")
    void create_duplicate_returns409() throws Exception {
        CreateConversationRequest request = new CreateConversationRequest(List.of(1L, 2L));
        when(conversationService.create(any(), any()))
                .thenThrow(new ConflictException("Une conversation existe déjà entre ces participants"));

        mockMvc.perform(post("/skillmatch/messagerie/conversations")
                        .requestAttr(AuthInterceptor.AUTHENTICATED_USER_ATTR, user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("POST /conversations — 400 validation (participantIds vide)")
    void create_emptyParticipants_returns400() throws Exception {
        String body = "{\"participantIds\":[]}";

        mockMvc.perform(post("/skillmatch/messagerie/conversations")
                        .requestAttr(AuthInterceptor.AUTHENTICATED_USER_ATTR, user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /conversations — 200 OK")
    void getMyConversations_returns200() throws Exception {
        ConversationResponse c = new ConversationResponse("conv-1", List.of(1L, 2L), 1L, LocalDateTime.now());
        when(conversationService.findByUser(any())).thenReturn(List.of(c));

        mockMvc.perform(get("/skillmatch/messagerie/conversations")
                        .requestAttr(AuthInterceptor.AUTHENTICATED_USER_ATTR, user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("conv-1"));
    }

    @Test
    @DisplayName("GET /conversations/{id} — 200 OK")
    void getById_returns200() throws Exception {
        ConversationResponse c = new ConversationResponse("conv-1", List.of(1L, 2L), 1L, null);
        when(conversationService.findById(eq("conv-1"), any())).thenReturn(c);

        mockMvc.perform(get("/skillmatch/messagerie/conversations/conv-1")
                        .requestAttr(AuthInterceptor.AUTHENTICATED_USER_ATTR, user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("conv-1"));
    }

    @Test
    @DisplayName("POST /conversations/{id}/messages — 201 Created")
    void sendMessage_returns201() throws Exception {
        SendMessageRequest request = new SendMessageRequest("Bonjour");
        MessageResponse response = new MessageResponse("m1", "Bonjour", LocalDateTime.now(), false, "conv-1", 1L);
        when(messageService.send(eq("conv-1"), any(), any())).thenReturn(response);

        mockMvc.perform(post("/skillmatch/messagerie/conversations/conv-1/messages")
                        .requestAttr(AuthInterceptor.AUTHENTICATED_USER_ATTR, user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("m1"))
                .andExpect(jsonPath("$.contenu").value("Bonjour"));
    }
}
