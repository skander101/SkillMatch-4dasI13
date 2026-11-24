package tn.esprit.skillmatch.messagerie.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.skillmatch.messagerie.config.AuthenticatedUser;
import tn.esprit.skillmatch.messagerie.config.UserClient;
import tn.esprit.skillmatch.messagerie.dto.CreateConversationRequest;
import tn.esprit.skillmatch.messagerie.dto.MessageResponse;
import tn.esprit.skillmatch.messagerie.dto.SendMessageRequest;
import tn.esprit.skillmatch.messagerie.entity.Conversation;
import tn.esprit.skillmatch.messagerie.entity.Message;
import tn.esprit.skillmatch.messagerie.exception.AccessForbiddenException;
import tn.esprit.skillmatch.messagerie.repository.ConversationRepository;
import tn.esprit.skillmatch.messagerie.repository.MessageRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests complets de la règle métier Admin sur :
 * - création de conversation directe
 * - création de groupe
 * - modification des participants
 * - envoi de message
 * Utilise une vraie instance d'ObjectMapper (jamais de mock).
 */
@ExtendWith(MockitoExtension.class)
class ConversationServiceAdminRuleTest {

    private static final String EXPECTED_ADMIN_MSG = "Vous ne pouvez pas écrire à un administrateur.";

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private UserClient userClient;

    @Mock
    private MessageEventPublisher eventPublisher;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private ConversationService conversationService;

    private MessageService messageService;

    private AuthenticatedUser candidat;
    private AuthenticatedUser candidat2;
    private AuthenticatedUser recruteur;
    private AuthenticatedUser admin;

    @BeforeEach
    void setUp() {
        candidat = new AuthenticatedUser(1L, "candidat@test.com", "CANDIDAT", "tok-c1");
        candidat2 = new AuthenticatedUser(3L, "candidat2@test.com", "CANDIDAT", "tok-c2");
        recruteur = new AuthenticatedUser(2L, "recruteur@test.com", "RECRUTEUR", "tok-r1");
        admin = new AuthenticatedUser(99L, "admin@test.com", "ADMIN", "tok-a1");
        messageService = new MessageService(messageRepository, conversationService, eventPublisher, userClient);
    }

    // ── Création de conversation directe ──

    @Test
    @DisplayName("Création directe : Candidat → Candidat autorisé")
    void createDirect_candidatVersCandidat_autorise() {
        when(conversationRepository.findByParticipantIds("[1,3]")).thenReturn(Optional.empty());
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userClient.resolveRole(3L, "tok-c1")).thenReturn("CANDIDAT");

        CreateConversationRequest req = new CreateConversationRequest(List.of(1L, 3L));
        assertDoesNotThrow(() -> conversationService.create(req, candidat));
    }

    @Test
    @DisplayName("Création directe : Recruteur → Candidat autorisé")
    void createDirect_recruteurVersCandidat_autorise() {
        when(conversationRepository.findByParticipantIds("[1,2]")).thenReturn(Optional.empty());
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userClient.resolveRole(1L, "tok-r1")).thenReturn("CANDIDAT");

        CreateConversationRequest req = new CreateConversationRequest(List.of(2L, 1L));
        assertDoesNotThrow(() -> conversationService.create(req, recruteur));
    }

    @Test
    @DisplayName("Création directe : Admin → Candidat et Admin → Recruteur autorisés")
    void createDirect_adminVersTous_autorise() {
        when(conversationRepository.findByParticipantIds(anyString())).thenReturn(Optional.empty());
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> conversationService.create(new CreateConversationRequest(List.of(99L, 1L)), admin));
        assertDoesNotThrow(() -> conversationService.create(new CreateConversationRequest(List.of(99L, 2L)), admin));
        assertDoesNotThrow(() -> conversationService.create(new CreateConversationRequest(List.of(99L, 100L)), admin));
    }

    @Test
    @DisplayName("Création directe : Candidat → Admin refusé (403)")
    void createDirect_candidatVersAdmin_refuse403() {
        when(userClient.resolveRole(99L, "tok-c1")).thenReturn("ADMIN");

        CreateConversationRequest req = new CreateConversationRequest(List.of(1L, 99L));
        AccessForbiddenException ex = assertThrows(AccessForbiddenException.class,
                () -> conversationService.create(req, candidat));
        assertEquals(EXPECTED_ADMIN_MSG, ex.getMessage());
        verify(conversationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Création directe : Recruteur → Admin refusé (403)")
    void createDirect_recruteurVersAdmin_refuse403() {
        when(userClient.resolveRole(99L, "tok-r1")).thenReturn("ADMIN");

        CreateConversationRequest req = new CreateConversationRequest(List.of(2L, 99L));
        AccessForbiddenException ex = assertThrows(AccessForbiddenException.class,
                () -> conversationService.create(req, recruteur));
        assertEquals(EXPECTED_ADMIN_MSG, ex.getMessage());
        verify(conversationRepository, never()).save(any());
    }

    // ── Création de groupe ──

    @Test
    @DisplayName("Création groupe : Candidat → Candidat + Recruteur autorisé")
    void createGroup_candidatAvecCandidatEtRecruteur_autorise() {
        when(conversationRepository.findByParticipantIds("[1,2,3]")).thenReturn(Optional.empty());
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userClient.resolveRole(2L, "tok-c1")).thenReturn("RECRUTEUR");
        when(userClient.resolveRole(3L, "tok-c1")).thenReturn("CANDIDAT");

        CreateConversationRequest req = new CreateConversationRequest(List.of(1L, 2L, 3L), "Équipe recrutement", "GROUPE");
        assertDoesNotThrow(() -> conversationService.create(req, candidat));
    }

    @Test
    @DisplayName("Création groupe : Recruteur → Candidats autorisé")
    void createGroup_recruteurAvecCandidats_autorise() {
        when(conversationRepository.findByParticipantIds("[1,2,3]")).thenReturn(Optional.empty());
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userClient.resolveRole(1L, "tok-r1")).thenReturn("CANDIDAT");
        when(userClient.resolveRole(3L, "tok-r1")).thenReturn("CANDIDAT");

        CreateConversationRequest req = new CreateConversationRequest(List.of(2L, 1L, 3L), "Entretiens", "GROUPE");
        assertDoesNotThrow(() -> conversationService.create(req, recruteur));
    }

    @Test
    @DisplayName("Création groupe : Admin → Candidat + Recruteur autorisé")
    void createGroup_adminVersTous_autorise() {
        when(conversationRepository.findByParticipantIds("[1,2,99]")).thenReturn(Optional.empty());
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateConversationRequest req = new CreateConversationRequest(List.of(99L, 1L, 2L), "Support", "GROUPE");
        assertDoesNotThrow(() -> conversationService.create(req, admin));
    }

    @Test
    @DisplayName("Création groupe : Candidat tente d'inclure un Admin → 403")
    void createGroup_candidatAvecAdmin_refuse403() {
        when(userClient.resolveRole(2L, "tok-c1")).thenReturn("RECRUTEUR");
        when(userClient.resolveRole(99L, "tok-c1")).thenReturn("ADMIN");

        CreateConversationRequest req = new CreateConversationRequest(List.of(1L, 2L, 99L), "Mon groupe", "GROUPE");
        AccessForbiddenException ex = assertThrows(AccessForbiddenException.class,
                () -> conversationService.create(req, candidat));
        assertEquals(EXPECTED_ADMIN_MSG, ex.getMessage());
        verify(conversationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Création groupe : Recruteur tente d'inclure un Admin → 403")
    void createGroup_recruteurAvecAdmin_refuse403() {
        when(userClient.resolveRole(1L, "tok-r1")).thenReturn("CANDIDAT");
        when(userClient.resolveRole(99L, "tok-r1")).thenReturn("ADMIN");

        CreateConversationRequest req = new CreateConversationRequest(List.of(2L, 1L, 99L), "Mon groupe", "GROUPE");
        AccessForbiddenException ex = assertThrows(AccessForbiddenException.class,
                () -> conversationService.create(req, recruteur));
        assertEquals(EXPECTED_ADMIN_MSG, ex.getMessage());
        verify(conversationRepository, never()).save(any());
    }

    // ── Modification des participants ──

    @Test
    @DisplayName("Modification participants : Candidat → ajoute Candidat autorisé")
    void updateParticipants_candidatAjouteCandidat_autorise() {
        Conversation groupe = new Conversation("g1", List.of(1L, 2L), 1L, "Équipe", Conversation.Type.GROUPE);
        when(conversationRepository.findById("g1")).thenReturn(Optional.of(groupe));
        when(conversationRepository.findByParticipantIds("[1,2,3]")).thenReturn(Optional.empty());
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userClient.resolveRole(2L, "tok-c1")).thenReturn("RECRUTEUR");
        when(userClient.resolveRole(3L, "tok-c1")).thenReturn("CANDIDAT");

        CreateConversationRequest req = new CreateConversationRequest(List.of(1L, 2L, 3L), "Équipe", "GROUPE");
        assertDoesNotThrow(() -> conversationService.updateParticipants("g1", req, candidat));
    }

    @Test
    @DisplayName("Modification participants : Recruteur → ajoute Candidat autorisé")
    void updateParticipants_recruteurAjouteCandidat_autorise() {
        Conversation conv = new Conversation("c1", List.of(2L, 3L), 2L);
        when(conversationRepository.findById("c1")).thenReturn(Optional.of(conv));
        when(conversationRepository.findByParticipantIds("[1,2,3]")).thenReturn(Optional.empty());
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userClient.resolveRole(1L, "tok-r1")).thenReturn("CANDIDAT");
        when(userClient.resolveRole(3L, "tok-r1")).thenReturn("CANDIDAT");

        CreateConversationRequest req = new CreateConversationRequest(List.of(2L, 3L, 1L), "Groupe RH", "GROUPE");
        assertDoesNotThrow(() -> conversationService.updateParticipants("c1", req, recruteur));
    }

    @Test
    @DisplayName("Modification participants : Admin → ajoute tout rôle autorisé")
    void updateParticipants_adminAjouteTous_autorise() {
        Conversation groupe = new Conversation("g1", List.of(99L, 1L), 99L, "Support", Conversation.Type.GROUPE);
        when(conversationRepository.findById("g1")).thenReturn(Optional.of(groupe));
        when(conversationRepository.findByParticipantIds("[1,2,99]")).thenReturn(Optional.empty());
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateConversationRequest req = new CreateConversationRequest(List.of(99L, 1L, 2L), "Support", "GROUPE");
        assertDoesNotThrow(() -> conversationService.updateParticipants("g1", req, admin));
    }

    @Test
    @DisplayName("Modification participants : Candidat tente d'ajouter un Admin → 403")
    void updateParticipants_candidatAjouteAdmin_refuse403() {
        Conversation groupe = new Conversation("g1", List.of(1L, 2L), 1L, "Mon groupe", Conversation.Type.GROUPE);
        when(conversationRepository.findById("g1")).thenReturn(Optional.of(groupe));
        when(userClient.resolveRole(2L, "tok-c1")).thenReturn("RECRUTEUR");
        when(userClient.resolveRole(99L, "tok-c1")).thenReturn("ADMIN");

        CreateConversationRequest req = new CreateConversationRequest(List.of(1L, 2L, 99L), "Mon groupe", "GROUPE");
        AccessForbiddenException ex = assertThrows(AccessForbiddenException.class,
                () -> conversationService.updateParticipants("g1", req, candidat));
        assertEquals(EXPECTED_ADMIN_MSG, ex.getMessage());
        verify(conversationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Modification participants : Recruteur tente d'ajouter un Admin → 403")
    void updateParticipants_recruteurAjouteAdmin_refuse403() {
        Conversation groupe = new Conversation("g1", List.of(1L, 2L), 2L, "Mon groupe", Conversation.Type.GROUPE);
        when(conversationRepository.findById("g1")).thenReturn(Optional.of(groupe));
        when(userClient.resolveRole(1L, "tok-r1")).thenReturn("CANDIDAT");
        when(userClient.resolveRole(99L, "tok-r1")).thenReturn("ADMIN");

        CreateConversationRequest req = new CreateConversationRequest(List.of(2L, 1L, 99L), "Mon groupe", "GROUPE");
        AccessForbiddenException ex = assertThrows(AccessForbiddenException.class,
                () -> conversationService.updateParticipants("g1", req, recruteur));
        assertEquals(EXPECTED_ADMIN_MSG, ex.getMessage());
        verify(conversationRepository, never()).save(any());
    }

    // ── Envoi de message ──

    @Test
    @DisplayName("Envoi de message : Candidat → Candidat autorisé")
    void sendMessage_candidatVersCandidat_autorise() {
        Conversation conv = new Conversation("c1", List.of(1L, 3L), 1L);
        when(conversationRepository.findById("c1")).thenReturn(Optional.of(conv));
        when(userClient.resolveRole(3L, "tok-c1")).thenReturn("CANDIDAT");
        when(userClient.resolveUsers(anyCollection(), eq("tok-c1")))
                .thenReturn(Map.of(1L, new UserClient.UserProfile(1L, "Attia", "CANDIDAT")));
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));

        MessageResponse resp = messageService.send("c1", new SendMessageRequest("Bonjour"), candidat);
        assertEquals("Bonjour", resp.contenu());
    }

    @Test
    @DisplayName("Envoi de message : Recruteur → Candidat autorisé")
    void sendMessage_recruteurVersCandidat_autorise() {
        Conversation conv = new Conversation("c1", List.of(1L, 2L), 2L);
        when(conversationRepository.findById("c1")).thenReturn(Optional.of(conv));
        when(userClient.resolveRole(1L, "tok-r1")).thenReturn("CANDIDAT");
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));

        MessageResponse resp = messageService.send("c1", new SendMessageRequest("Entretien demain"), recruteur);
        assertEquals("Entretien demain", resp.contenu());
    }

    @Test
    @DisplayName("Envoi de message : Admin → tous autorisé")
    void sendMessage_adminVersTous_autorise() {
        Conversation conv = new Conversation("c1", List.of(1L, 2L, 99L), 99L, "Support", Conversation.Type.GROUPE);
        when(conversationRepository.findById("c1")).thenReturn(Optional.of(conv));
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> messageService.send("c1", new SendMessageRequest("Message admin"), admin));
    }

    @Test
    @DisplayName("Envoi de message : Candidat dans conversation avec Admin → 403")
    void sendMessage_candidatVersAdmin_refuse403() {
        Conversation conv = new Conversation("c1", List.of(1L, 99L), 99L);
        when(conversationRepository.findById("c1")).thenReturn(Optional.of(conv));
        when(userClient.resolveRole(99L, "tok-c1")).thenReturn("ADMIN");

        AccessForbiddenException ex = assertThrows(AccessForbiddenException.class,
                () -> messageService.send("c1", new SendMessageRequest("Bonjour admin"), candidat));
        assertEquals(EXPECTED_ADMIN_MSG, ex.getMessage());
        verify(messageRepository, never()).save(any());
    }

    @Test
    @DisplayName("Envoi de message : Recruteur dans conversation avec Admin → 403")
    void sendMessage_recruteurVersAdmin_refuse403() {
        Conversation conv = new Conversation("c1", List.of(2L, 99L), 99L);
        when(conversationRepository.findById("c1")).thenReturn(Optional.of(conv));
        when(userClient.resolveRole(99L, "tok-r1")).thenReturn("ADMIN");

        AccessForbiddenException ex = assertThrows(AccessForbiddenException.class,
                () -> messageService.send("c1", new SendMessageRequest("Bonjour admin"), recruteur));
        assertEquals(EXPECTED_ADMIN_MSG, ex.getMessage());
        verify(messageRepository, never()).save(any());
    }
}
