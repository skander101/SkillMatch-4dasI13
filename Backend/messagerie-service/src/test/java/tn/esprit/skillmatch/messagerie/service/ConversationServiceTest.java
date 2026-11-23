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
import tn.esprit.skillmatch.messagerie.dto.ConversationResponse;
import tn.esprit.skillmatch.messagerie.dto.CreateConversationRequest;
import tn.esprit.skillmatch.messagerie.entity.Conversation;
import tn.esprit.skillmatch.messagerie.exception.AccessForbiddenException;
import tn.esprit.skillmatch.messagerie.exception.BadRequestException;
import tn.esprit.skillmatch.messagerie.exception.ConflictException;
import tn.esprit.skillmatch.messagerie.exception.ResourceNotFoundException;
import tn.esprit.skillmatch.messagerie.repository.ConversationRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private ConversationService conversationService;

    private AuthenticatedUser candidat;
    private AuthenticatedUser recruteur;
    private AuthenticatedUser admin;

    @BeforeEach
    void setUp() {
        candidat = new AuthenticatedUser(1L, "candidat@test.com", "CANDIDAT");
        recruteur = new AuthenticatedUser(2L, "recruteur@test.com", "RECRUTEUR");
        admin = new AuthenticatedUser(99L, "admin@test.com", "ADMIN");
    }

    @Test
    @DisplayName("Créer une conversation — succès")
    void create_success() {
        CreateConversationRequest request = new CreateConversationRequest(List.of(1L, 2L));
        when(conversationRepository.findByParticipantIds(anyString())).thenReturn(Optional.empty());
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> inv.getArgument(0));

        ConversationResponse response = conversationService.create(request, candidat);

        assertNotNull(response.id());
        assertEquals(List.of(1L, 2L), response.participantIds());
        assertEquals(1L, response.candidatUserId());
        verify(conversationRepository).save(any(Conversation.class));
    }

    @Test
    @DisplayName("Créer une conversation — un seul participant distinct → 400")
    void create_singleParticipant_throws400() {
        CreateConversationRequest request = new CreateConversationRequest(List.of(1L, 1L));

        assertThrows(BadRequestException.class, () -> conversationService.create(request, candidat));
        verify(conversationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Créer une conversation — créateur absent de la liste → 400")
    void create_creatorNotInList_throws400() {
        CreateConversationRequest request = new CreateConversationRequest(List.of(2L, 3L));

        assertThrows(BadRequestException.class, () -> conversationService.create(request, candidat));
        verify(conversationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Créer une conversation — doublon → 409")
    void create_duplicate_throws409() {
        CreateConversationRequest request = new CreateConversationRequest(List.of(1L, 2L));
        Conversation existing = new Conversation("existing-id", List.of(1L, 2L), 1L);
        when(conversationRepository.findByParticipantIds(anyString())).thenReturn(Optional.of(existing));

        assertThrows(ConflictException.class, () -> conversationService.create(request, candidat));
        verify(conversationRepository, never()).save(any());
    }

    @Test
    @DisplayName("findByUser — utilisateur normal voit ses conversations")
    void findByUser_normal() {
        Conversation c = new Conversation("c1", List.of(1L, 2L), 1L);
        c.setDernierMessageAt(LocalDateTime.now());
        when(conversationRepository.findByParticipantId("1")).thenReturn(List.of(c));

        List<ConversationResponse> result = conversationService.findByUser(candidat);

        assertEquals(1, result.size());
        assertEquals("c1", result.get(0).id());
    }

    @Test
    @DisplayName("findByUser — ADMIN voit toutes les conversations")
    void findByUser_admin() {
        Conversation c1 = new Conversation("c1", List.of(1L, 2L), 1L);
        Conversation c2 = new Conversation("c2", List.of(3L, 4L), 3L);
        when(conversationRepository.findAll()).thenReturn(List.of(c1, c2));

        List<ConversationResponse> result = conversationService.findByUser(admin);

        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("findById — non participant → 403")
    void findById_notParticipant_throws403() {
        Conversation c = new Conversation("c1", List.of(1L, 2L), 1L);
        when(conversationRepository.findById("c1")).thenReturn(Optional.of(c));
        AuthenticatedUser outsider = new AuthenticatedUser(99L, "outsider@test.com", "CANDIDAT");

        assertThrows(AccessForbiddenException.class, () -> conversationService.findById("c1", outsider));
    }

    @Test
    @DisplayName("findById — conversation introuvable → 404")
    void findById_notFound_throws404() {
        when(conversationRepository.findById("unknown")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> conversationService.findById("unknown", candidat));
    }
}
