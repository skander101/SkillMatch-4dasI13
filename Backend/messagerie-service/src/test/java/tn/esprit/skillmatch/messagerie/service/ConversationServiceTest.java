package tn.esprit.skillmatch.messagerie.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import tn.esprit.skillmatch.messagerie.config.AuthenticatedUser;
import tn.esprit.skillmatch.messagerie.config.UserClient;
import tn.esprit.skillmatch.messagerie.dto.ConversationResponse;
import tn.esprit.skillmatch.messagerie.dto.CreateConversationRequest;
import tn.esprit.skillmatch.messagerie.dto.FichierUploadResponse;
import tn.esprit.skillmatch.messagerie.entity.Conversation;
import tn.esprit.skillmatch.messagerie.entity.Message;
import tn.esprit.skillmatch.messagerie.exception.AccessForbiddenException;
import tn.esprit.skillmatch.messagerie.exception.BadRequestException;
import tn.esprit.skillmatch.messagerie.exception.ConflictException;
import tn.esprit.skillmatch.messagerie.exception.ResourceNotFoundException;
import tn.esprit.skillmatch.messagerie.repository.ConversationRepository;
import tn.esprit.skillmatch.messagerie.repository.MessageRepository;

import java.nio.file.Path;
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
class ConversationServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private UserClient userClient;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private ConversationService conversationService;

    private AuthenticatedUser candidat;
    private AuthenticatedUser recruteur;
    private AuthenticatedUser admin;

    @BeforeEach
    void setUp() {
        candidat = new AuthenticatedUser(1L, "candidat@test.com", "CANDIDAT", "tok-1");
        recruteur = new AuthenticatedUser(2L, "recruteur@test.com", "RECRUTEUR", "tok-2");
        admin = new AuthenticatedUser(99L, "admin@test.com", "ADMIN", "tok-99");
    }

    @Test
    @DisplayName("Créer une conversation directe — succès")
    void create_success() {
        CreateConversationRequest request = new CreateConversationRequest(List.of(1L, 2L));
        when(conversationRepository.findByParticipantIds(anyString())).thenReturn(Optional.empty());
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userClient.resolveUsers(anyCollection(), anyString())).thenReturn(Map.of(
                1L, new UserClient.UserProfile(1L, "Attia", "CANDIDAT"),
                2L, new UserClient.UserProfile(2L, "Meriem", "RECRUTEUR")
        ));

        ConversationResponse response = conversationService.create(request, candidat);

        assertNotNull(response.id());
        assertEquals(List.of(1L, 2L), response.participantIds());
        assertEquals(1L, response.candidatUserId());
        assertEquals("direct", response.type());
        assertFalse(response.estGroupe());
        assertEquals(2, response.participants().size());
        assertEquals("Meriem", response.participants().get(1).nom());
        verify(conversationRepository).save(any(Conversation.class));
    }

    @Test
    @DisplayName("Créer un groupe — persiste le message système de création")
    void create_group_savesSystemMessage() {
        CreateConversationRequest request = new CreateConversationRequest(List.of(1L, 2L, 3L), "Équipe recrutement", "GROUPE");
        when(conversationRepository.findByParticipantIds(anyString())).thenReturn(Optional.empty());
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userClient.resolveUsers(anyCollection(), anyString())).thenReturn(Map.of(
                1L, new UserClient.UserProfile(1L, "Attia Ben", "CANDIDAT"),
                2L, new UserClient.UserProfile(2L, "Meriem", "RECRUTEUR"),
                3L, new UserClient.UserProfile(3L, "Sara", "RECRUTEUR")
        ));

        ConversationResponse response = conversationService.create(request, candidat);

        assertEquals("groupe", response.type());
        assertTrue(response.estGroupe());
        assertEquals("Équipe recrutement", response.nomGroupe());

        ArgumentCaptor<Message> msgCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(msgCaptor.capture());
        assertEquals(Message.TYPE_SYSTEME, msgCaptor.getValue().getType());
        assertEquals("Attia a créé le groupe", msgCaptor.getValue().getContenu());
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
    @DisplayName("findByUser — ADMIN voit uniquement ses propres conversations (pas findAll)")
    void findByUser_admin_onlyOwnConversations() {
        Conversation c1 = new Conversation("c1", List.of(1L, 99L), 99L);
        when(conversationRepository.findByParticipantId("99")).thenReturn(List.of(c1));

        List<ConversationResponse> result = conversationService.findByUser(admin);

        assertEquals(1, result.size());
        assertEquals("c1", result.get(0).id());
        verify(conversationRepository, never()).findAll();
        verify(conversationRepository).findByParticipantId("99");
    }

    @Test
    @DisplayName("Quitter groupe – transfert du créateur lorsque le créateur quitte")
    void quitterGroupe_creatorTransfer() {
        // Setup a group with three participants, creator is candidat (id 1)
        CreateConversationRequest createReq = new CreateConversationRequest(List.of(1L, 2L, 3L), "Équipe recrutement", "GROUPE");
        // Mock repository behavior
        Conversation conv = new Conversation("c1", List.of(1L, 2L, 3L), 1L);
        conv.setNomGroupe("Équipe recrutement");
        conv.setType(Conversation.Type.GROUPE.name());
        when(conversationRepository.save(any(Conversation.class))).thenReturn(conv);
        when(conversationRepository.findByParticipantIds(anyString())).thenReturn(Optional.empty());
        when(userClient.resolveUsers(anyCollection(), anyString())).thenReturn(Map.of(
                1L, new UserClient.UserProfile(1L, "Attia", "CANDIDAT"),
                2L, new UserClient.UserProfile(2L, "Meriem", "RECRUTEUR"),
                3L, new UserClient.UserProfile(3L, "Sara", "RECRUTEUR")
        ));
        // Create the group first
        conversationService.create(createReq, candidat);
        // Now creator leaves the group
        when(conversationRepository.findById("c1")).thenReturn(Optional.of(conv));
        ConversationResponse resp = conversationService.quitterGroupe("c1", candidat);
        // Verify that the creator has been transferred to the oldest remaining participant (Meriem, id 2)
        assertEquals(2L, resp.candidatUserId());
    }

    @Test
    @DisplayName("findById — non participant → 403")
    void findById_notParticipant_throws403() {
        Conversation c = new Conversation("c1", List.of(1L, 2L), 1L);
        when(conversationRepository.findById("c1")).thenReturn(Optional.of(c));
        AuthenticatedUser outsider = new AuthenticatedUser(55L, "outsider@test.com", "CANDIDAT");

        assertThrows(AccessForbiddenException.class, () -> conversationService.findById("c1", outsider));
    }

    @Test
    @DisplayName("findById — conversation introuvable → 404")
    void findById_notFound_throws404() {
        when(conversationRepository.findById("unknown")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> conversationService.findById("unknown", candidat));
    }

    @Test
    @DisplayName("getEntityById — retourne l'entité")
    void getEntityById_success() {
        Conversation c = new Conversation("c1", List.of(1L, 2L), 1L);
        when(conversationRepository.findById("c1")).thenReturn(Optional.of(c));

        assertSame(c, conversationService.getEntityById("c1"));
    }

    @Test
    @DisplayName("Modifier les participants — succès")
    void updateParticipants_success() {
        Conversation conversation = new Conversation("c1", List.of(1L, 2L), 1L);
        when(conversationRepository.findById("c1")).thenReturn(Optional.of(conversation));
        when(conversationRepository.findByParticipantIds(anyString())).thenReturn(Optional.empty());
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> inv.getArgument(0));

        ConversationResponse response = conversationService.updateParticipants(
                "c1", new CreateConversationRequest(List.of(1L, 3L)), candidat);

        assertEquals(List.of(1L, 3L), response.participantIds());
        verify(conversationRepository).save(conversation);
    }

    @Test
    @DisplayName("Quitter le groupe par le créateur — transfère le rôle au plus ancien non-admin et crée message système")
    void quitterGroupe_creatorLeaves_transfersCreatorToOldestNonAdmin() {
        Conversation groupe = new Conversation("g1", List.of(1L, 2L, 3L, 99L), 1L, "Équipe", Conversation.Type.GROUPE);
        // 99L (Admin) est avant 2L (Recruteur) et 3L (Candidat) dans l'ordre : le transfert doit ignorer 99L et choisir 2L
        groupe.setOrdreParticipants(List.of(1L, 99L, 2L, 3L));

        when(conversationRepository.findById("g1")).thenReturn(Optional.of(groupe));
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userClient.resolveUsers(anyCollection(), eq("tok-1"))).thenReturn(Map.of(
                1L, new UserClient.UserProfile(1L, "Attia", "CANDIDAT"),
                99L, new UserClient.UserProfile(99L, "Admin", "ADMIN"),
                2L, new UserClient.UserProfile(2L, "Meriem", "RECRUTEUR"),
                3L, new UserClient.UserProfile(3L, "Sara", "CANDIDAT")
        ));

        ConversationResponse response = conversationService.quitterGroupe("g1", candidat);

        assertEquals(2L, response.candidatUserId());
        assertEquals(List.of(2L, 3L, 99L), response.participantIds());
        assertFalse(response.participantIds().contains(1L));

        ArgumentCaptor<Message> msgCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(msgCaptor.capture());
        assertEquals(Message.TYPE_SYSTEME, msgCaptor.getValue().getType());
        assertEquals("Attia a quitté le groupe", msgCaptor.getValue().getContenu());
    }

    @Test
    @DisplayName("Quitter une conversation directe → 400")
    void quitterGroupe_directConversation_throws400() {
        Conversation direct = new Conversation("d1", List.of(1L, 2L), 1L);
        when(conversationRepository.findById("d1")).thenReturn(Optional.of(direct));

        assertThrows(BadRequestException.class, () -> conversationService.quitterGroupe("d1", candidat));
    }

    @Test
    @DisplayName("Quitter un groupe avec seulement 2 membres → 400 (propose conversion directe)")
    void quitterGroupe_twoMembersRemainingOne_throws400() {
        Conversation groupe = new Conversation("g1", List.of(1L, 2L), 1L, "Petit groupe", Conversation.Type.GROUPE);
        when(conversationRepository.findById("g1")).thenReturn(Optional.of(groupe));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> conversationService.quitterGroupe("g1", candidat));
        assertTrue(ex.getMessage().contains("conversation directe"));
    }

    @Test
    @DisplayName("Upload fichier — rejette traversée ../ et extensions interdites (.html, .svg)")
    void uploadFichier_securityChecks(@TempDir Path tempDir) {
        ReflectionTestUtils.setField(conversationService, "fichiersDir", tempDir.toString());
        Conversation c = new Conversation("c1", List.of(1L, 2L), 1L);
        when(conversationRepository.findById("c1")).thenReturn(Optional.of(c));

        MockMultipartFile traversal = new MockMultipartFile("fichier", "../secret.pdf", "application/pdf", "ok".getBytes());
        assertThrows(BadRequestException.class, () -> conversationService.uploadFichier("c1", traversal, candidat));

        MockMultipartFile svg = new MockMultipartFile("fichier", "icon.svg", "image/svg+xml", "<svg/>".getBytes());
        assertThrows(BadRequestException.class, () -> conversationService.uploadFichier("c1", svg, candidat));

        MockMultipartFile html = new MockMultipartFile("fichier", "page.html", "text/html", "<html/>".getBytes());
        assertThrows(BadRequestException.class, () -> conversationService.uploadFichier("c1", html, candidat));

        MockMultipartFile valid = new MockMultipartFile("fichier", "cv.pdf", "application/pdf", "pdfdata".getBytes());
        FichierUploadResponse resp = conversationService.uploadFichier("c1", valid, candidat);
        assertTrue(resp.url().startsWith("/skillmatch/messagerie/conversations/c1/fichiers/"));
        assertEquals("cv.pdf", resp.nom());
    }

    @Test
    @DisplayName("Supprimer une conversation — supprime aussi ses messages")
    void delete_success() {
        Conversation conversation = new Conversation("c1", List.of(1L, 2L), 1L);
        when(conversationRepository.findById("c1")).thenReturn(Optional.of(conversation));

        conversationService.delete("c1", candidat);

        verify(messageRepository).deleteByConversationId("c1");
        verify(conversationRepository).delete(conversation);
    }

    @Test
    @DisplayName("Supprimer une conversation — non créateur interdit")
    void delete_notCreator_throws403() {
        Conversation conversation = new Conversation("c1", List.of(1L, 2L), 1L);
        when(conversationRepository.findById("c1")).thenReturn(Optional.of(conversation));

        assertThrows(AccessForbiddenException.class, () -> conversationService.delete("c1", recruteur));
        verify(messageRepository, never()).deleteByConversationId(anyString());
    }
}
