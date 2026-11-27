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
import tn.esprit.skillmatch.messagerie.dto.CreateSignalementRequest;
import tn.esprit.skillmatch.messagerie.dto.SignalementResponse;
import tn.esprit.skillmatch.messagerie.dto.TraiterSignalementRequest;
import tn.esprit.skillmatch.messagerie.exception.AccessForbiddenException;
import tn.esprit.skillmatch.messagerie.service.SignalementService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SignalementController.class)
@Import({WebConfig.class, CurrentUserArgumentResolver.class})
class SignalementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SignalementService signalementService;

    @MockBean
    private AuthInterceptor authInterceptor;

    private final AuthenticatedUser candidat = new AuthenticatedUser(1L, "c@test.com", "CANDIDAT");
    private final AuthenticatedUser admin = new AuthenticatedUser(99L, "a@test.com", "ADMIN");

    @BeforeEach
    void setUp() throws Exception {
        when(authInterceptor.preHandle(any(), any(), any())).thenReturn(true);
    }

    @Test
    @DisplayName("POST /messages/{id}/signalements — 201 Created")
    void signalerMessage_returns201() throws Exception {
        SignalementResponse response = new SignalementResponse(
                "sig-1", "conv-1", "msg-1",
                "Meriem", "CANDIDAT", "Yassine", "RECRUTEUR",
                "Spam", "Liens répétés", "EN_ATTENTE", null, null,
                LocalDateTime.now(), null, null, false
        );
        when(signalementService.signalerMessage(eq("msg-1"), any(), any())).thenReturn(response);

        mockMvc.perform(post("/skillmatch/messagerie/messages/msg-1/signalements")
                        .requestAttr(AuthInterceptor.AUTHENTICATED_USER_ATTR, candidat)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateSignalementRequest("Spam", "Liens répétés"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.motif").value("Spam"))
                .andExpect(jsonPath("$.statut").value("EN_ATTENTE"));
    }

    @Test
    @DisplayName("GET /admin/signalements — 403 si non admin")
    void listAdmin_nonAdmin_returns403() throws Exception {
        when(signalementService.listForAdmin(any(), any()))
                .thenThrow(new AccessForbiddenException("Cette fonctionnalité est réservée aux administrateurs."));

        mockMvc.perform(get("/skillmatch/messagerie/admin/signalements")
                        .requestAttr(AuthInterceptor.AUTHENTICATED_USER_ATTR, candidat))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /admin/signalements/{id}/consulter — 200 OK")
    void consulter_returns200() throws Exception {
        SignalementResponse response = new SignalementResponse(
                "sig-1", "conv-1", "msg-1",
                "Meriem", "CANDIDAT", "Yassine", "RECRUTEUR",
                "Spam", null, "EN_ATTENTE", null, null,
                LocalDateTime.now(), null, "Texte signalé", false
        );
        when(signalementService.consulterMessageSignale(eq("sig-1"), any())).thenReturn(response);

        mockMvc.perform(post("/skillmatch/messagerie/admin/signalements/sig-1/consulter")
                        .requestAttr(AuthInterceptor.AUTHENTICATED_USER_ATTR, admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messageContenu").value("Texte signalé"));
    }

    @Test
    @DisplayName("PATCH /admin/signalements/{id}/traiter — 200 OK")
    void traiter_returns200() throws Exception {
        SignalementResponse response = new SignalementResponse(
                "sig-1", "conv-1", "msg-1",
                "Meriem", "CANDIDAT", "Yassine", "RECRUTEUR",
                "Spam", null, "MESSAGE_MASQUE", "MASQUER_MESSAGE", "Admin",
                LocalDateTime.now(), LocalDateTime.now(), null, true
        );
        when(signalementService.traiterSignalement(eq("sig-1"), any(), any())).thenReturn(response);

        mockMvc.perform(patch("/skillmatch/messagerie/admin/signalements/sig-1/traiter")
                        .requestAttr(AuthInterceptor.AUTHENTICATED_USER_ATTR, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TraiterSignalementRequest("MASQUER_MESSAGE", "Retiré"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("MESSAGE_MASQUE"))
                .andExpect(jsonPath("$.messageMasque").value(true));
    }
}
