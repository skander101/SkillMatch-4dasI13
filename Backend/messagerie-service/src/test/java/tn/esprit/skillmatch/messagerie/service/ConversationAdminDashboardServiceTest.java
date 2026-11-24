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
import tn.esprit.skillmatch.messagerie.dto.AdminConversationDetailResponse;
import tn.esprit.skillmatch.messagerie.dto.AdminConversationSummaryResponse;
import tn.esprit.skillmatch.messagerie.dto.ConversationModerationRequest;
import tn.esprit.skillmatch.messagerie.dto.MessagerieAdminDashboardResponse;
import tn.esprit.skillmatch.messagerie.entity.Conversation;
import tn.esprit.skillmatch.messagerie.entity.Message;
import tn.esprit.skillmatch.messagerie.exception.AccessForbiddenException;
import tn.esprit.skillmatch.messagerie.exception.ResourceNotFoundException;
import tn.esprit.skillmatch.messagerie.repository.ConversationRepository;
import tn.esprit.skillmatch.messagerie.repository.MessageRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationAdminDashboardServiceTest {

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

    private AuthenticatedUser admin;
    private AuthenticatedUser candidat;

    @BeforeEach
    void setUp() {
        admin = new AuthenticatedUser(99L, "admin@test.com", "ADMIN", "tok-admin");
        candidat = new AuthenticatedUser(1L, "candidat@test.com", "CANDIDAT", "tok-candidat");
    }

    @Test
    @DisplayName("Dashboard admin — retourne KPI et conversations filtrées")
    void getAdminDashboard_returnsStatsAndFilteredRows() {
        Conversation groupe = new Conversation("g1", List.of(1L, 2L, 3L), 2L, "Equipe recrutement", Conversation.Type.GROUPE);
        groupe.setOrdreParticipants(List.of(2L, 1L, 3L));
        groupe.setDernierMessageAt(LocalDateTime.of(2026, 11, 19, 11, 0));
        groupe.setSignalee(true);

        Conversation archivee = new Conversation("d1", List.of(4L, 5L), 4L);
        archivee.setOrdreParticipants(List.of(4L, 5L));
        archivee.setDernierMessageAt(LocalDateTime.of(2026, 11, 18, 9, 0));
        archivee.setArchivee(true);

        when(conversationRepository.findAllByOrderByDernierMessageAtDescDateCreationDesc()).thenReturn(List.of(groupe, archivee));
        when(userClient.resolveUsers(anyCollection(), anyString())).thenReturn(Map.of(
                1L, new UserClient.UserProfile(1L, "Meriem", "CANDIDAT"),
                2L, new UserClient.UserProfile(2L, "Yassine", "RECRUTEUR"),
                3L, new UserClient.UserProfile(3L, "Sarra", "CANDIDAT"),
                4L, new UserClient.UserProfile(4L, "TechNova", "RECRUTEUR"),
                5L, new UserClient.UserProfile(5L, "Lina", "CANDIDAT")
        ));
        when(messageRepository.countByDateGreaterThanEqualAndDateLessThan(any(), any())).thenReturn(342L);
        when(messageRepository.countByConversationId("g1")).thenReturn(87L);
        when(messageRepository.countByConversationId("d1")).thenReturn(24L);
        when(messageRepository.countByConversationIdAndLuFalseAndSenderIdNot("g1", 99L)).thenReturn(2L);
        when(messageRepository.countByConversationIdAndLuFalseAndSenderIdNot("d1", 99L)).thenReturn(0L);
        when(messageRepository.findFirstByConversationIdOrderByDateDesc("g1"))
                .thenReturn(Optional.of(new Message("m1", "Merci pour votre retour", LocalDateTime.of(2026, 11, 19, 11, 0), groupe, 2L)));
        when(messageRepository.findFirstByConversationIdOrderByDateDesc("d1"))
                .thenReturn(Optional.of(new Message("m2", "Entretien confirmé", LocalDateTime.of(2026, 11, 18, 9, 0), archivee, 4L)));

        MessagerieAdminDashboardResponse response = conversationService.getAdminDashboard("GROUPES", "meriem", admin);

        assertEquals(1L, response.stats().conversations());
        assertEquals(1L, response.stats().groupes());
        assertEquals(342L, response.stats().messagesAujourdhui());
        assertEquals(1L, response.stats().signalementsATraiter());
        assertEquals(1, response.conversations().size());
        assertEquals("Equipe recrutement", response.conversations().get(0).conversation());
        assertEquals("signalee", response.conversations().get(0).statut());
        assertEquals(87L, response.conversations().get(0).messages());
    }

    @Test
    @DisplayName("Dashboard admin — refuse un utilisateur non admin")
    void getAdminDashboard_nonAdmin_forbidden() {
        assertThrows(AccessForbiddenException.class,
                () -> conversationService.getAdminDashboard(null, null, candidat));
        verify(conversationRepository, never()).findAllByOrderByDernierMessageAtDescDateCreationDesc();
    }

    @Test
    @DisplayName("Modération admin — met à jour signalement et archivage")
    void updateModeration_updatesFlags() {
        Conversation conversation = new Conversation("c1", List.of(1L, 2L), 1L);
        conversation.setOrdreParticipants(List.of(1L, 2L));
        when(conversationRepository.findById("c1")).thenReturn(Optional.of(conversation));
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userClient.resolveUsers(anyCollection(), anyString())).thenReturn(Map.of(
                1L, new UserClient.UserProfile(1L, "Meriem", "CANDIDAT"),
                2L, new UserClient.UserProfile(2L, "Yassine", "RECRUTEUR")
        ));
        when(messageRepository.countByConversationId("c1")).thenReturn(0L);
        when(messageRepository.countByConversationIdAndLuFalseAndSenderIdNot("c1", 99L)).thenReturn(0L);
        when(messageRepository.findFirstByConversationIdOrderByDateDesc("c1")).thenReturn(Optional.empty());

        AdminConversationSummaryResponse response = conversationService.updateModeration(
                "c1",
                new ConversationModerationRequest(true, true),
                admin
        );

        assertTrue(response.signalee());
        assertTrue(response.archivee());
        assertEquals("archivee", response.statut());
        assertTrue(conversation.getSignalee());
        assertTrue(conversation.getArchivee());
        assertFalse(conversation.getDateSignalement() == null);
        assertFalse(conversation.getDateArchivage() == null);
    }

    @Test
    @DisplayName("Dashboard admin — calcule les nouveautés de la semaine et l'heure de pic")
    void getAdminDashboard_computesWeeklyStatsAndPeakHour() {
        LocalDateTime now = LocalDateTime.now();
        Conversation groupeRecent = new Conversation("g-new", List.of(1L, 2L, 3L), 2L, "Stage 2026", Conversation.Type.GROUPE);
        groupeRecent.setDateCreation(now);

        Conversation directRecent = new Conversation("d-new", List.of(1L, 2L), 1L);
        directRecent.setDateCreation(now);

        Conversation directAncien = new Conversation("d-old", List.of(3L, 4L), 3L);
        directAncien.setDateCreation(now.minusWeeks(2));

        Message m1 = new Message("m1", "Salut", now.withHour(11).withMinute(5), groupeRecent, 1L);
        Message m2 = new Message("m2", "Bonjour", now.withHour(11).withMinute(25), directRecent, 2L);
        Message m3 = new Message("m3", "Ok", now.withHour(15).withMinute(10), directRecent, 1L);

        when(conversationRepository.findAllByOrderByDernierMessageAtDescDateCreationDesc())
                .thenReturn(List.of(groupeRecent, directRecent, directAncien));
        when(userClient.resolveUsers(anyCollection(), anyString())).thenReturn(Map.of(
                1L, new UserClient.UserProfile(1L, "Meriem", "CANDIDAT"),
                2L, new UserClient.UserProfile(2L, "Yassine", "RECRUTEUR"),
                3L, new UserClient.UserProfile(3L, "Sarra", "CANDIDAT"),
                4L, new UserClient.UserProfile(4L, "Lina", "CANDIDAT")
        ));
        when(messageRepository.countByDateGreaterThanEqualAndDateLessThan(any(), any())).thenReturn(3L);
        when(messageRepository.findByDateGreaterThanEqualAndDateLessThan(any(), any())).thenReturn(List.of(m1, m2, m3));

        MessagerieAdminDashboardResponse response = conversationService.getAdminDashboard(null, null, "GROUPE", "active", admin);

        assertEquals(2L, response.stats().conversations());
        assertEquals(1L, response.stats().groupes());
        assertEquals(1L, response.stats().nouvellesConversationsSemaine());
        assertEquals(1L, response.stats().nouveauxGroupesSemaine());
        assertEquals(3L, response.stats().messagesAujourdhui());
        assertEquals(11, response.stats().heurePicActivite());
        assertEquals("Pic à 11 h", response.stats().heurePicLibelle());
        assertEquals(1, response.conversations().size());
        assertEquals("g-new", response.conversations().get(0).id());
    }

    @Test
    @DisplayName("Détail admin conversation — retourne métadonnées, participants et messages")
    void getAdminConversationDetail_returnsDetailAndMessages() {
        Conversation conv = new Conversation("c-detail", List.of(1L, 2L), 1L);
        conv.setOrdreParticipants(List.of(1L, 2L));
        Message msg = new Message("m1", "Voici mon CV", LocalDateTime.of(2026, 11, 25, 10, 15), conv, 1L);

        when(conversationRepository.findById("c-detail")).thenReturn(Optional.of(conv));
        when(messageRepository.findByConversationIdOrderByDateAsc("c-detail")).thenReturn(List.of(msg));
        when(messageRepository.findFirstByConversationIdOrderByDateDesc("c-detail")).thenReturn(Optional.of(msg));
        when(messageRepository.countByConversationId("c-detail")).thenReturn(1L);
        when(messageRepository.countByConversationIdAndLuFalseAndSenderIdNot("c-detail", 99L)).thenReturn(0L);
        when(userClient.resolveUsers(anyCollection(), anyString())).thenReturn(Map.of(
                1L, new UserClient.UserProfile(1L, "Meriem", "CANDIDAT"),
                2L, new UserClient.UserProfile(2L, "Yassine", "RECRUTEUR")
        ));

        AdminConversationDetailResponse detail = conversationService.getAdminConversationDetail("c-detail", admin);

        assertEquals("c-detail", detail.conversation().id());
        assertEquals(2, detail.conversation().participants().size());
        assertEquals(1, detail.messages().size());
        assertEquals("Meriem", detail.messages().get(0).senderNom());
        assertEquals("Voici mon CV", detail.messages().get(0).contenu());
    }

    @Test
    @DisplayName("Détail admin conversation — refuse non admin (403) et introuvable (404)")
    void getAdminConversationDetail_checksRoleAndExistence() {
        assertThrows(AccessForbiddenException.class,
                () -> conversationService.getAdminConversationDetail("c-1", candidat));

        when(conversationRepository.findById("unknown")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> conversationService.getAdminConversationDetail("unknown", admin));
    }

    @Test
    @DisplayName("Export CSV admin — inclut BOM UTF-8, participants et statut")
    void exportAdminDashboardCsv_returnsFormattedCsv() {
        Conversation conv = new Conversation("g1", List.of(1L, 2L, 3L), 1L, "Equipe recrutement", Conversation.Type.GROUPE);
        conv.setDernierMessageAt(LocalDateTime.of(2026, 11, 25, 11, 30));

        when(conversationRepository.findAllByOrderByDernierMessageAtDescDateCreationDesc()).thenReturn(List.of(conv));
        when(userClient.resolveUsers(anyCollection(), anyString())).thenReturn(Map.of(
                1L, new UserClient.UserProfile(1L, "Meriem", "CANDIDAT"),
                2L, new UserClient.UserProfile(2L, "Yassine", "RECRUTEUR"),
                3L, new UserClient.UserProfile(3L, "Sarra", "CANDIDAT")
        ));
        when(messageRepository.countByConversationId("g1")).thenReturn(87L);
        when(messageRepository.findFirstByConversationIdOrderByDateDesc("g1"))
                .thenReturn(Optional.of(new Message("m1", "Bonjour", LocalDateTime.of(2026, 11, 25, 11, 30), conv, 1L)));

        String csv = conversationService.exportAdminDashboardCsv("GROUPES", "equipe", admin);

        assertTrue(csv.startsWith("\uFEFFID;Conversation;Type;Membres;Participants;Messages;"));
        assertTrue(csv.contains("\"Equipe recrutement\";\"groupe\";3;\"Meriem, Yassine, Sarra\";87;\"2026-11-25 11:30:00\";\"active\""));
    }
}
