package tn.esprit.skillmatch.messagerie.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.skillmatch.messagerie.config.AuthenticatedUser;
import tn.esprit.skillmatch.messagerie.config.UserClient;
import tn.esprit.skillmatch.messagerie.dto.MessageResponse;
import tn.esprit.skillmatch.messagerie.dto.SendMessageRequest;
import tn.esprit.skillmatch.messagerie.dto.UpdateMessageRequest;
import tn.esprit.skillmatch.messagerie.entity.Conversation;
import tn.esprit.skillmatch.messagerie.entity.Message;
import tn.esprit.skillmatch.messagerie.exception.AccessForbiddenException;
import tn.esprit.skillmatch.messagerie.exception.BadRequestException;
import tn.esprit.skillmatch.messagerie.exception.ResourceNotFoundException;
import tn.esprit.skillmatch.messagerie.repository.MessageRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ConversationService conversationService;

    @Mock
    private MessageEventPublisher eventPublisher;

    @Mock
    private UserClient userClient;

    @InjectMocks
    private MessageService messageService;

    private AuthenticatedUser sender;
    private AuthenticatedUser recipient;
    private AuthenticatedUser outsider;
    private Conversation conversation;

    @BeforeEach
    void setUp() {
        sender = new AuthenticatedUser(1L, "sender@test.com", "CANDIDAT", "tok-1");
        recipient = new AuthenticatedUser(2L, "recipient@test.com", "RECRUTEUR", "tok-2");
        outsider = new AuthenticatedUser(99L, "outsider@test.com", "CANDIDAT", "tok-99");
        conversation = new Conversation("conv-1", List.of(1L, 2L), 1L);
    }

    @Test
    @DisplayName("Envoyer un message — succès + événement publié")
    void send_success() {
        SendMessageRequest request = new SendMessageRequest("Bonjour");
        when(conversationService.getEntityById("conv-1")).thenReturn(conversation);
        when(userClient.resolveRole(2L, "tok-1")).thenReturn("RECRUTEUR");
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));

        MessageResponse response = messageService.send("conv-1", request, sender);

        assertNotNull(response.id());
        assertEquals("Bonjour", response.contenu());
        assertEquals("texte", response.type());
        assertEquals(1L, response.senderId());
        assertEquals("conv-1", response.conversationId());
        assertFalse(response.lu());
        verify(eventPublisher).publish(anyString(), eq("conv-1"), eq(1L), eq(List.of(1L, 2L)), eq("Bonjour"), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Envoyer un message de type systeme depuis le client → 400")
    void send_systemTypeFromClient_throws400() {
        SendMessageRequest request = new SendMessageRequest("Fake system", "systeme", null, null, null);
        when(conversationService.getEntityById("conv-1")).thenReturn(conversation);
        when(userClient.resolveRole(2L, "tok-1")).thenReturn("RECRUTEUR");

        assertThrows(BadRequestException.class, () -> messageService.send("conv-1", request, sender));
        verify(messageRepository, never()).save(any());
    }

    @Test
    @DisplayName("Envoyer un message — non participant → 403")
    void send_nonParticipant_throws403() {
        SendMessageRequest request = new SendMessageRequest("Hello");
        when(conversationService.getEntityById("conv-1")).thenReturn(conversation);

        assertThrows(AccessForbiddenException.class, () -> messageService.send("conv-1", request, outsider));
        verify(messageRepository, never()).save(any());
    }

    @Test
    @DisplayName("Lister les messages — triés par date croissante")
    void listByConversation_ordered() {
        when(conversationService.getEntityById("conv-1")).thenReturn(conversation);
        Message m1 = new Message("m1", "Premier", LocalDateTime.of(2026, 1, 1, 10, 0), conversation, 1L);
        Message m2 = new Message("m2", "Deuxième", LocalDateTime.of(2026, 1, 1, 11, 0), conversation, 2L);
        when(messageRepository.findByConversationIdOrderByDateAsc("conv-1")).thenReturn(List.of(m1, m2));

        List<MessageResponse> result = messageService.listByConversation("conv-1", sender);

        assertEquals(2, result.size());
        assertEquals("Premier", result.get(0).contenu());
        assertEquals("Deuxième", result.get(1).contenu());
    }

    @Test
    @DisplayName("Modifier un message — par l'expéditeur → succès")
    void update_bySender_success() {
        Message message = new Message("m1", "Ancien", LocalDateTime.now(), conversation, 1L);
        when(messageRepository.findById("m1")).thenReturn(Optional.of(message));
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));

        MessageResponse response = messageService.update("m1", new UpdateMessageRequest("Nouveau"), sender);

        assertEquals("Nouveau", response.contenu());
    }

    @Test
    @DisplayName("Modifier un message — par un non-expéditeur → 403")
    void update_byNonSender_throws403() {
        Message message = new Message("m1", "Ancien", LocalDateTime.now(), conversation, 1L);
        when(messageRepository.findById("m1")).thenReturn(Optional.of(message));

        assertThrows(AccessForbiddenException.class,
                () -> messageService.update("m1", new UpdateMessageRequest("Nouveau"), recipient));
    }

    @Test
    @DisplayName("Supprimer un message — par l'expéditeur → succès")
    void delete_bySender_success() {
        Message message = new Message("m1", "Contenu", LocalDateTime.now(), conversation, 1L);
        when(messageRepository.findById("m1")).thenReturn(Optional.of(message));

        messageService.delete("m1", sender);

        verify(messageRepository).delete(message);
    }

    @Test
    @DisplayName("Supprimer un message — par un non-expéditeur → 403")
    void delete_byNonSender_throws403() {
        Message message = new Message("m1", "Contenu", LocalDateTime.now(), conversation, 1L);
        when(messageRepository.findById("m1")).thenReturn(Optional.of(message));

        assertThrows(AccessForbiddenException.class, () -> messageService.delete("m1", recipient));
        verify(messageRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Marquer comme lu — par un destinataire → succès")
    void markAsRead_byRecipient_success() {
        Message message = new Message("m1", "Contenu", LocalDateTime.now(), conversation, 1L);
        when(messageRepository.findById("m1")).thenReturn(Optional.of(message));
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));

        MessageResponse response = messageService.markAsRead("m1", recipient);

        assertTrue(response.lu());
    }

    @Test
    @DisplayName("Marquer comme lu — par l'expéditeur → 403")
    void markAsRead_bySender_throws403() {
        Message message = new Message("m1", "Contenu", LocalDateTime.now(), conversation, 1L);
        when(messageRepository.findById("m1")).thenReturn(Optional.of(message));

        assertThrows(AccessForbiddenException.class, () -> messageService.markAsRead("m1", sender));
    }

    @Test
    @DisplayName("Marquer comme lu — non participant → 403")
    void markAsRead_nonParticipant_throws403() {
        Message message = new Message("m1", "Contenu", LocalDateTime.now(), conversation, 1L);
        when(messageRepository.findById("m1")).thenReturn(Optional.of(message));

        assertThrows(AccessForbiddenException.class, () -> messageService.markAsRead("m1", outsider));
    }

    @Test
    @DisplayName("Message introuvable → 404")
    void findById_notFound_throws404() {
        when(messageRepository.findById("unknown")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> messageService.findById("unknown", sender));
    }
}
