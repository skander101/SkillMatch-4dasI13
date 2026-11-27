package tn.esprit.skillmatch.messagerie.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.skillmatch.messagerie.config.AuthenticatedUser;
import tn.esprit.skillmatch.messagerie.config.UserClient;
import tn.esprit.skillmatch.messagerie.dto.CreateSignalementRequest;
import tn.esprit.skillmatch.messagerie.dto.SendMessageRequest;
import tn.esprit.skillmatch.messagerie.dto.SignalementResponse;
import tn.esprit.skillmatch.messagerie.dto.TraiterSignalementRequest;
import tn.esprit.skillmatch.messagerie.entity.AuditLog;
import tn.esprit.skillmatch.messagerie.entity.Conversation;
import tn.esprit.skillmatch.messagerie.entity.Message;
import tn.esprit.skillmatch.messagerie.entity.Signalement;
import tn.esprit.skillmatch.messagerie.entity.SuspensionEnvoi;
import tn.esprit.skillmatch.messagerie.exception.AccessForbiddenException;
import tn.esprit.skillmatch.messagerie.exception.BadRequestException;
import tn.esprit.skillmatch.messagerie.exception.ConflictException;
import tn.esprit.skillmatch.messagerie.repository.AuditLogRepository;
import tn.esprit.skillmatch.messagerie.repository.ConversationRepository;
import tn.esprit.skillmatch.messagerie.repository.MessageRepository;
import tn.esprit.skillmatch.messagerie.repository.SignalementRepository;
import tn.esprit.skillmatch.messagerie.repository.SuspensionEnvoiRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SignalementServiceTest {

    @Mock
    private SignalementRepository signalementRepository;
    @Mock
    private MessageRepository messageRepository;
    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private SuspensionEnvoiRepository suspensionEnvoiRepository;
    @Mock
    private UserClient userClient;
    @Mock
    private ConversationService conversationService;
    @Mock
    private MessageEventPublisher eventPublisher;

    @InjectMocks
    private SignalementService signalementService;

    private AuthenticatedUser candidat;
    private AuthenticatedUser recruteur;
    private AuthenticatedUser admin;
    private Conversation conversation;
    private Message message;

    @BeforeEach
    void setUp() {
        candidat = new AuthenticatedUser(1L, "c@test.com", "CANDIDAT", "tok-c");
        recruteur = new AuthenticatedUser(2L, "r@test.com", "RECRUTEUR", "tok-r");
        admin = new AuthenticatedUser(99L, "a@test.com", "ADMIN", "tok-a");
        conversation = new Conversation("conv-1", List.of(1L, 2L), 1L);
        message = new Message("msg-1", "Contenu abusif", LocalDateTime.now(), conversation, 2L);
    }

    @Test
    @DisplayName("Signaler un message — crée le signalement, marque message/conversation et écrit dans l'audit")
    void signalerMessage_success() {
        when(messageRepository.findById("msg-1")).thenReturn(Optional.of(message));
        when(signalementRepository.existsByMessageIdAndAuteurSignalementId("msg-1", 1L)).thenReturn(false);
        when(signalementRepository.save(any(Signalement.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userClient.resolveUsers(anyCollection(), anyString())).thenReturn(Map.of(
                1L, new UserClient.UserProfile(1L, "Meriem", "CANDIDAT"),
                2L, new UserClient.UserProfile(2L, "Yassine", "RECRUTEUR")
        ));

        SignalementResponse response = signalementService.signalerMessage(
                "msg-1", new CreateSignalementRequest("Harcèlement", "Propos insistants"), candidat);

        assertEquals("Harcèlement", response.motif());
        assertEquals("EN_ATTENTE", response.statut());
        assertTrue(message.getSignalee());
        assertTrue(conversation.getSignalee());
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Signaler son propre message → 400 ; doublon → 409")
    void signalerMessage_validations() {
        when(messageRepository.findById("msg-1")).thenReturn(Optional.of(message));
        assertThrows(BadRequestException.class, () ->
                signalementService.signalerMessage("msg-1", new CreateSignalementRequest("Spam", null), recruteur));

        when(signalementRepository.existsByMessageIdAndAuteurSignalementId("msg-1", 1L)).thenReturn(true);
        assertThrows(ConflictException.class, () ->
                signalementService.signalerMessage("msg-1", new CreateSignalementRequest("Spam", null), candidat));
    }

    @Test
    @DisplayName("Consultation d'un message signalé par ADMIN — écrit dans le journal d'audit")
    void consulterMessageSignale_writesAuditLog() {
        Signalement sig = new Signalement("sig-1", "conv-1", "msg-1", 1L, 2L, "Spam", null, LocalDateTime.now());
        when(signalementRepository.findById("sig-1")).thenReturn(Optional.of(sig));
        when(messageRepository.findById("msg-1")).thenReturn(Optional.of(message));
        when(userClient.resolveUsers(anyCollection(), anyString())).thenReturn(Map.of(
                1L, new UserClient.UserProfile(1L, "Meriem", "CANDIDAT"),
                2L, new UserClient.UserProfile(2L, "Yassine", "RECRUTEUR"),
                99L, new UserClient.UserProfile(99L, "Admin", "ADMIN")
        ));

        SignalementResponse response = signalementService.consulterMessageSignale("sig-1", admin);

        assertEquals("Contenu abusif", response.messageContenu());
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertEquals("CONSULTATION_MESSAGE_SIGNALE", captor.getValue().getAction());
        assertEquals("conv-1", captor.getValue().getConversationId());
    }

    @Test
    @DisplayName("Endpoints admin signalement — 403 si non admin")
    void adminEndpoints_nonAdmin_throws403() {
        assertThrows(AccessForbiddenException.class, () -> signalementService.listForAdmin(null, candidat));
        assertThrows(AccessForbiddenException.class, () -> signalementService.consulterMessageSignale("sig-1", recruteur));
        assertThrows(AccessForbiddenException.class, () ->
                signalementService.traiterSignalement("sig-1", new TraiterSignalementRequest("IGNORER", null), candidat));
    }

    @Test
    @DisplayName("Traiter un signalement : MASQUER_MESSAGE et SUSPENDRE_ENVOI")
    void traiterSignalement_actions() {
        Signalement sig = new Signalement("sig-1", "conv-1", "msg-1", 1L, 2L, "Spam", null, LocalDateTime.now());
        when(signalementRepository.findById("sig-1")).thenReturn(Optional.of(sig));
        when(messageRepository.findById("msg-1")).thenReturn(Optional.of(message));
        when(conversationRepository.findById("conv-1")).thenReturn(Optional.of(conversation));
        when(signalementRepository.save(any(Signalement.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userClient.resolveUsers(anyCollection(), anyString())).thenReturn(Map.of());

        SignalementResponse masked = signalementService.traiterSignalement(
                "sig-1", new TraiterSignalementRequest("MASQUER_MESSAGE", "Contenu retiré"), admin);
        assertEquals("MESSAGE_MASQUE", masked.statut());
        assertTrue(message.getMasque());

        when(suspensionEnvoiRepository.existsByUserIdAndActiveTrue(2L)).thenReturn(false);
        SignalementResponse suspended = signalementService.traiterSignalement(
                "sig-1", new TraiterSignalementRequest("SUSPENDRE_ENVOI", "Récidive"), admin);
        assertEquals("ENVOI_SUSPENDU", suspended.statut());
        verify(suspensionEnvoiRepository).save(any(SuspensionEnvoi.class));
    }

    @Test
    @DisplayName("Utilisateur suspendu ne peut plus envoyer de message (403)")
    void suspendedUser_cannotSendMessage() {
        MessageService msgService = new MessageService(
                messageRepository, conversationService, eventPublisher, userClient, suspensionEnvoiRepository);
        when(conversationService.getEntityById("conv-1")).thenReturn(conversation);
        when(suspensionEnvoiRepository.existsByUserIdAndActiveTrue(2L)).thenReturn(true);

        assertThrows(AccessForbiddenException.class, () ->
                msgService.send("conv-1", new SendMessageRequest("Test"), recruteur));
        verify(messageRepository, never()).save(any());
    }
}
