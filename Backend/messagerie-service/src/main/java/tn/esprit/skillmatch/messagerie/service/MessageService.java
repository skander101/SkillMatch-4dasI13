package tn.esprit.skillmatch.messagerie.service;

import org.springframework.stereotype.Service;
import tn.esprit.skillmatch.messagerie.config.AuthenticatedUser;
import tn.esprit.skillmatch.messagerie.dto.MessageResponse;
import tn.esprit.skillmatch.messagerie.dto.SendMessageRequest;
import tn.esprit.skillmatch.messagerie.dto.UpdateMessageRequest;
import tn.esprit.skillmatch.messagerie.entity.Conversation;
import tn.esprit.skillmatch.messagerie.entity.Message;
import tn.esprit.skillmatch.messagerie.exception.AccessForbiddenException;
import tn.esprit.skillmatch.messagerie.exception.ResourceNotFoundException;
import tn.esprit.skillmatch.messagerie.repository.MessageRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final ConversationService conversationService;
    private final MessageEventPublisher eventPublisher;

    public MessageService(MessageRepository messageRepository,
                          ConversationService conversationService,
                          MessageEventPublisher eventPublisher) {
        this.messageRepository = messageRepository;
        this.conversationService = conversationService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Envoie un message dans une conversation.
     * - L'utilisateur doit être participant de la conversation.
     * - lu = false, date = maintenant, senderId = user authentifié.
     * - Met à jour dernierMessageAt sur la conversation.
     * - Publie un événement RabbitMQ (async, ne bloque pas en cas d'échec).
     */
    public MessageResponse send(String conversationId, SendMessageRequest request, AuthenticatedUser user) {
        Conversation conversation = conversationService.getEntityById(conversationId);
        checkParticipant(conversation, user);

        LocalDateTime now = LocalDateTime.now();

        Message message = new Message(
                UUID.randomUUID().toString(),
                request.contenu(),
                now,
                conversation,
                user.id()
        );

        Message saved = messageRepository.save(message);

        // Mettre à jour le timestamp du dernier message
        conversation.setDernierMessageAt(now);
        // La conversation est gérée par JPA (managed entity), save via le repo
        // On utilise getEntityById qui retourne l'entité attachée, donc le flush suffit
        // mais pour être explicite, on fait un save indirect via le message.conversation

        // Publier l'événement (async, ne bloque pas)
        eventPublisher.publish(
                saved.getId(),
                conversationId,
                user.id(),
                conversation.getParticipantIds(),
                saved.getContenu(),
                now
        );

        return toResponse(saved);
    }

    /**
     * Liste les messages d'une conversation (triés par date croissante).
     * L'utilisateur doit être participant (sauf ADMIN).
     */
    public List<MessageResponse> listByConversation(String conversationId, AuthenticatedUser user) {
        Conversation conversation = conversationService.getEntityById(conversationId);
        checkParticipant(conversation, user);

        return messageRepository.findByConversationIdOrderByDateAsc(conversationId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Récupère un message par son id.
     * L'utilisateur doit être participant de la conversation du message (sauf ADMIN).
     */
    public MessageResponse findById(String id, AuthenticatedUser user) {
        Message message = getMessageOrThrow(id);
        checkParticipant(message.getConversation(), user);
        return toResponse(message);
    }

    /**
     * Modifie le contenu d'un message.
     * Seul l'expéditeur peut modifier son propre message.
     */
    public MessageResponse update(String id, UpdateMessageRequest request, AuthenticatedUser user) {
        Message message = getMessageOrThrow(id);
        checkSender(message, user);

        message.setContenu(request.contenu());
        Message saved = messageRepository.save(message);
        return toResponse(saved);
    }

    /**
     * Supprime un message.
     * Seul l'expéditeur peut supprimer son propre message.
     */
    public void delete(String id, AuthenticatedUser user) {
        Message message = getMessageOrThrow(id);
        checkSender(message, user);
        messageRepository.delete(message);
    }

    /**
     * Marque un message comme lu.
     * Seul un destinataire (participant mais pas l'expéditeur) peut marquer comme lu.
     */
    public MessageResponse markAsRead(String id, AuthenticatedUser user) {
        Message message = getMessageOrThrow(id);
        Conversation conversation = message.getConversation();

        checkParticipant(conversation, user);

        if (message.getSenderId().equals(user.id())) {
            throw new AccessForbiddenException("L'expéditeur ne peut pas marquer son propre message comme lu");
        }

        message.marquerCommeLu();
        Message saved = messageRepository.save(message);
        return toResponse(saved);
    }

    // ── helpers ──

    private Message getMessageOrThrow(String id) {
        return messageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Message introuvable : " + id));
    }

    private void checkParticipant(Conversation conversation, AuthenticatedUser user) {
        if (!user.isAdmin() && !conversation.estParticipant(user.id())) {
            throw new AccessForbiddenException("Vous n'êtes pas participant de cette conversation");
        }
    }

    private void checkSender(Message message, AuthenticatedUser user) {
        if (!message.getSenderId().equals(user.id())) {
            throw new AccessForbiddenException("Seul l'expéditeur peut effectuer cette action");
        }
    }

    private MessageResponse toResponse(Message m) {
        return new MessageResponse(
                m.getId(),
                m.getContenu(),
                m.getDate(),
                m.getLu(),
                m.getConversation().getId(),
                m.getSenderId()
        );
    }
}
