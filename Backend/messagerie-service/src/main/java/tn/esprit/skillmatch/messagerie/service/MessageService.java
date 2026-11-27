package tn.esprit.skillmatch.messagerie.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import tn.esprit.skillmatch.messagerie.repository.SuspensionEnvoiRepository;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final ConversationService conversationService;
    private final MessageEventPublisher eventPublisher;
    private final UserClient userClient;
    private final SuspensionEnvoiRepository suspensionEnvoiRepository;

    public MessageService(MessageRepository messageRepository,
                          ConversationService conversationService,
                          MessageEventPublisher eventPublisher,
                          UserClient userClient) {
        this(messageRepository, conversationService, eventPublisher, userClient, null);
    }

    @Autowired
    public MessageService(MessageRepository messageRepository,
                          ConversationService conversationService,
                          MessageEventPublisher eventPublisher,
                          UserClient userClient,
                          SuspensionEnvoiRepository suspensionEnvoiRepository) {
        this.messageRepository = messageRepository;
        this.conversationService = conversationService;
        this.eventPublisher = eventPublisher;
        this.userClient = userClient;
        this.suspensionEnvoiRepository = suspensionEnvoiRepository;
    }

    /**
     * Envoie un message dans une conversation.
     * - L'utilisateur doit être participant de la conversation.
     * - Un Candidat ou Recruteur ne peut pas envoyer dans une conversation contenant un Admin (403).
     * - Les messages système sont générés côté serveur uniquement.
     */
    @Transactional
    public MessageResponse send(String conversationId, SendMessageRequest request, AuthenticatedUser user) {
        Conversation conversation = conversationService.getEntityById(conversationId);
        checkParticipant(conversation, user);

        if (suspensionEnvoiRepository != null && suspensionEnvoiRepository.existsByUserIdAndActiveTrue(user.id())) {
            throw new AccessForbiddenException("Votre droit d'envoi de messages a été suspendu par un administrateur.");
        }

        if (!user.isAdmin() && userClient != null) {
            for (Long participantId : conversation.getParticipantIds()) {
                if (participantId.equals(user.id())) continue;
                String role = userClient.resolveRole(participantId, user.token());
                if ("ADMIN".equalsIgnoreCase(role)) {
                    throw new AccessForbiddenException(ConversationService.MSG_FORBIDDEN_ADMIN);
                }
            }
        }

        if (request.type() != null && (
                "systeme".equalsIgnoreCase(request.type().trim())
                        || "système".equalsIgnoreCase(request.type().trim()))) {
            throw new BadRequestException("Les messages système sont générés automatiquement.");
        }

        String type = Message.TYPE_FICHIER.equalsIgnoreCase(request.type())
                ? Message.TYPE_FICHIER
                : Message.TYPE_TEXTE;

        LocalDateTime now = LocalDateTime.now();

        Message message = new Message(
                UUID.randomUUID().toString(),
                request.contenu(),
                type,
                now,
                false,
                conversation,
                user.id(),
                Message.TYPE_FICHIER.equals(type) ? request.fichierNom() : null,
                Message.TYPE_FICHIER.equals(type) ? request.fichierUrl() : null,
                Message.TYPE_FICHIER.equals(type) ? request.fichierTaille() : null
        );

        Message saved = messageRepository.save(message);
        conversation.setDernierMessageAt(now);

        eventPublisher.publish(
                saved.getId(),
                conversationId,
                user.id(),
                conversation.getParticipantIds(),
                saved.getContenu(),
                now
        );

        Map<Long, UserClient.UserProfile> usersMap = resolveSenders(List.of(user.id()), user);
        return toResponse(saved, usersMap);
    }

    /**
     * Liste les messages d'une conversation (triés par date croissante).
     */
    public List<MessageResponse> listByConversation(String conversationId, AuthenticatedUser user) {
        Conversation conversation = conversationService.getEntityById(conversationId);
        checkParticipant(conversation, user);

        List<Message> messages = messageRepository.findByConversationIdOrderByDateAsc(conversationId);
        Set<Long> senderIds = new LinkedHashSet<>();
        for (Message m : messages) {
            if (!m.estSysteme() && m.getSenderId() != null && m.getSenderId() > 0) {
                senderIds.add(m.getSenderId());
            }
        }
        Map<Long, UserClient.UserProfile> usersMap = resolveSenders(senderIds, user);

        return messages.stream()
                .map(m -> toResponse(m, usersMap))
                .collect(Collectors.toList());
    }

    /**
     * Récupère un message par son id.
     */
    public MessageResponse findById(String id, AuthenticatedUser user) {
        Message message = getMessageOrThrow(id);
        checkParticipant(message.getConversation(), user);
        Map<Long, UserClient.UserProfile> usersMap = !message.estSysteme() && message.getSenderId() != null
                ? resolveSenders(List.of(message.getSenderId()), user)
                : Map.of();
        return toResponse(message, usersMap);
    }

    /**
     * Modifie le contenu d'un message.
     * Seul l'expéditeur peut modifier son propre message (hors message système).
     */
    public MessageResponse update(String id, UpdateMessageRequest request, AuthenticatedUser user) {
        Message message = getMessageOrThrow(id);
        if (message.estSysteme()) {
            throw new AccessForbiddenException("Un message système ne peut pas être modifié.");
        }
        checkSender(message, user);

        message.setContenu(request.contenu());
        Message saved = messageRepository.save(message);
        Map<Long, UserClient.UserProfile> usersMap = resolveSenders(List.of(saved.getSenderId()), user);
        return toResponse(saved, usersMap);
    }

    /**
     * Supprime un message.
     * Seul l'expéditeur peut supprimer son propre message (hors message système).
     */
    public void delete(String id, AuthenticatedUser user) {
        Message message = getMessageOrThrow(id);
        if (message.estSysteme()) {
            throw new AccessForbiddenException("Un message système ne peut pas être supprimé.");
        }
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

        if (message.estSysteme()) {
            return toResponse(message, Map.of());
        }

        if (message.getSenderId().equals(user.id())) {
            throw new AccessForbiddenException("L'expéditeur ne peut pas marquer son propre message comme lu.");
        }

        message.marquerCommeLu();
        Message saved = messageRepository.save(message);
        Map<Long, UserClient.UserProfile> usersMap = resolveSenders(List.of(saved.getSenderId()), user);
        return toResponse(saved, usersMap);
    }

    private Map<Long, UserClient.UserProfile> resolveSenders(java.util.Collection<Long> ids, AuthenticatedUser user) {
        if (userClient == null || ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return userClient.resolveUsers(ids, user != null ? user.token() : null);
    }

    private Message getMessageOrThrow(String id) {
        return messageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Message introuvable : " + id));
    }

    private void checkParticipant(Conversation conversation, AuthenticatedUser user) {
        if (!user.isAdmin() && !conversation.estParticipant(user.id())) {
            throw new AccessForbiddenException("Vous n'êtes pas participant de cette conversation.");
        }
    }

    private void checkSender(Message message, AuthenticatedUser user) {
        if (!message.getSenderId().equals(user.id())) {
            throw new AccessForbiddenException("Seul l'expéditeur peut effectuer cette action.");
        }
    }

    private MessageResponse toResponse(Message m, Map<Long, UserClient.UserProfile> usersMap) {
        String senderNom = null;
        if (!m.estSysteme() && m.getSenderId() != null) {
            UserClient.UserProfile profile = usersMap.get(m.getSenderId());
            if (profile != null) {
                senderNom = profile.nom();
            }
        }
        boolean masque = Boolean.TRUE.equals(m.getMasque());
        boolean signalee = Boolean.TRUE.equals(m.getSignalee());
        String contenu = masque ? "[Message masqué par la modération]" : m.getContenu();
        return new MessageResponse(
                m.getId(),
                contenu,
                m.getType() != null ? m.getType() : Message.TYPE_TEXTE,
                m.getDate(),
                m.getLu(),
                m.getConversation().getId(),
                m.getSenderId(),
                senderNom,
                masque ? null : m.getFichierNom(),
                masque ? null : m.getFichierUrl(),
                masque ? null : m.getFichierTaille(),
                signalee,
                masque
        );
    }
}
