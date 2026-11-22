package tn.esprit.skillmatch.messagerie.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import tn.esprit.skillmatch.messagerie.config.AuthenticatedUser;
import tn.esprit.skillmatch.messagerie.dto.ConversationResponse;
import tn.esprit.skillmatch.messagerie.dto.CreateConversationRequest;
import tn.esprit.skillmatch.messagerie.entity.Conversation;
import tn.esprit.skillmatch.messagerie.exception.AccessForbiddenException;
import tn.esprit.skillmatch.messagerie.exception.BadRequestException;
import tn.esprit.skillmatch.messagerie.exception.ConflictException;
import tn.esprit.skillmatch.messagerie.exception.ResourceNotFoundException;
import tn.esprit.skillmatch.messagerie.repository.ConversationRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ObjectMapper objectMapper;

    public ConversationService(ConversationRepository conversationRepository, ObjectMapper objectMapper) {
        this.conversationRepository = conversationRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Crée une conversation entre les participants indiqués.
     * Règles :
     * - Au moins 2 participants distincts.
     * - Le créateur (user authentifié) doit figurer dans la liste.
     * - Les ids sont triés pour la détection de doublons.
     * - Si une conversation existe déjà entre exactement ces participants → 409.
     */
    public ConversationResponse create(CreateConversationRequest request, AuthenticatedUser user) {
        List<Long> ids = request.participantIds().stream()
                .distinct()
                .collect(Collectors.toList());

        if (ids.size() < 2) {
            throw new BadRequestException("Il faut au moins 2 participants distincts");
        }

        if (!ids.contains(user.id())) {
            throw new BadRequestException("Le créateur doit figurer dans la liste des participants");
        }

        // Trier pour la détection de doublons
        List<Long> sortedIds = new ArrayList<>(ids);
        Collections.sort(sortedIds);

        // Vérifier l'existence d'un doublon
        String sortedJson = toJson(sortedIds);
        conversationRepository.findByParticipantIds(sortedJson).ifPresent(existing -> {
            throw new ConflictException("Une conversation existe déjà entre ces participants");
        });

        Conversation conversation = new Conversation(
                UUID.randomUUID().toString(),
                sortedIds,
                user.id()
        );

        Conversation saved = conversationRepository.save(conversation);
        return toResponse(saved);
    }

    /**
     * Liste les conversations de l'utilisateur courant.
     * Un ADMIN peut voir toutes les conversations.
     */
    public List<ConversationResponse> findByUser(AuthenticatedUser user) {
        if (user.isAdmin()) {
            return conversationRepository.findAll().stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());
        }
        return conversationRepository.findByParticipantId(user.id().toString()).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Récupère une conversation par son id.
     * Vérifie que l'utilisateur est participant (sauf ADMIN).
     */
    public ConversationResponse findById(String id, AuthenticatedUser user) {
        Conversation conversation = getConversationOrThrow(id);
        checkParticipant(conversation, user);
        return toResponse(conversation);
    }

    /**
     * Récupère l'entité Conversation (usage interne pour MessageService).
     */
    public Conversation getEntityById(String id) {
        return getConversationOrThrow(id);
    }

    // ── helpers ──

    private Conversation getConversationOrThrow(String id) {
        return conversationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation introuvable : " + id));
    }

    private void checkParticipant(Conversation conversation, AuthenticatedUser user) {
        if (!user.isAdmin() && !conversation.estParticipant(user.id())) {
            throw new AccessForbiddenException("Vous n'êtes pas participant de cette conversation");
        }
    }

    private ConversationResponse toResponse(Conversation c) {
        return new ConversationResponse(c.getId(), c.getParticipantIds(), c.getCandidatUserId(), c.getDernierMessageAt());
    }

    private String toJson(List<Long> ids) {
        try {
            return objectMapper.writeValueAsString(ids);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Erreur de sérialisation JSON", e);
        }
    }
}
