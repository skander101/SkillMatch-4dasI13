package tn.esprit.skillmatch.entreprises.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.skillmatch.entreprises.client.UtilisateurInfo;
import tn.esprit.skillmatch.entreprises.dto.AvisRequest;
import tn.esprit.skillmatch.entreprises.dto.AvisResponse;
import tn.esprit.skillmatch.entreprises.dto.ModerationRequest;
import tn.esprit.skillmatch.entreprises.entity.AvisEntreprise;
import tn.esprit.skillmatch.entreprises.entity.Entreprise;
import tn.esprit.skillmatch.entreprises.entity.StatutModeration;
import tn.esprit.skillmatch.entreprises.event.EvenementEntreprise;
import tn.esprit.skillmatch.entreprises.mapper.AvisMapper;
import tn.esprit.skillmatch.entreprises.repository.AvisEntrepriseRepository;
import tn.esprit.skillmatch.entreprises.repository.EntrepriseRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvisServiceTest {

    @Mock
    private AvisEntrepriseRepository avisEntrepriseRepository;

    @Mock
    private EntrepriseRepository entrepriseRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    private final AvisMapper avisMapper = new AvisMapper();

    private AvisService avisService;

    @BeforeEach
    void setUp() {
        avisService = new AvisService(
                avisEntrepriseRepository,
                entrepriseRepository,
                avisMapper,
                applicationEventPublisher
        );
    }

    @Test
    void submitCreatesAvisWithAuthenticatedUser() {
        Entreprise entreprise = entreprise(1L);
        UtilisateurInfo utilisateur = utilisateur("CANDIDAT");
        AvisRequest request = new AvisRequest(5, " Super ");

        when(entrepriseRepository.findById(1L)).thenReturn(Optional.of(entreprise));
        when(avisEntrepriseRepository.existsByEntreprise_IdAndUtilisateurId(1L, 10L)).thenReturn(false);
        when(avisEntrepriseRepository.saveAndFlush(any(AvisEntreprise.class))).thenAnswer(invocation -> {
            AvisEntreprise saved = invocation.getArgument(0);
            saved.setId(2L);
            return saved;
        });

        AvisResponse response = avisService.submit(1L, utilisateur, request);

        assertEquals(2L, response.id());
        assertEquals(1L, response.entrepriseId());
        assertEquals(10L, response.utilisateurId());
        assertEquals("Alice Martin", response.nomUtilisateur());
        assertEquals("Super", response.commentaire());
        assertEquals(5, response.note());
        assertEquals(StatutModeration.EN_ATTENTE, response.statut());
        assertNotNull(response.dateCreation());
    }

    @Test
    void submitRejectsDuplicateAvis() {
        Entreprise entreprise = entreprise(1L);
        UtilisateurInfo utilisateur = utilisateur("CANDIDAT");
        AvisRequest request = new AvisRequest(5, "Super");

        when(entrepriseRepository.findById(1L)).thenReturn(Optional.of(entreprise));
        when(avisEntrepriseRepository.existsByEntreprise_IdAndUtilisateurId(1L, 10L)).thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                avisService.submit(1L, utilisateur, request)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(avisEntrepriseRepository, never()).saveAndFlush(any(AvisEntreprise.class));
    }

    @Test
    void submitRejectsUnknownEntreprise() {
        UtilisateurInfo utilisateur = utilisateur("CANDIDAT");
        AvisRequest request = new AvisRequest(5, "Super");

        when(entrepriseRepository.findById(1L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                avisService.submit(1L, utilisateur, request)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void findByEntrepriseReturnsMappedAvis() {
        Entreprise entreprise = entreprise(1L);
        AvisEntreprise avis = avis(2L, entreprise);
        AvisResponse expected = response(avis);

        when(entrepriseRepository.existsById(1L)).thenReturn(true);
        when(avisEntrepriseRepository.findByEntreprise_IdOrderByDateCreationDesc(1L)).thenReturn(List.of(avis));

        assertEquals(List.of(expected), avisService.findByEntreprise(1L));
    }

    @Test
    void findByEntrepriseRejectsUnknownEntreprise() {
        when(entrepriseRepository.existsById(1L)).thenReturn(false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                avisService.findByEntreprise(1L)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void moderateApprovesAvisAndRecalculatesAverage() {
        Entreprise entreprise = entreprise(1L);
        AvisEntreprise avis = avis(2L, entreprise);

        when(avisEntrepriseRepository.findById(2L)).thenReturn(Optional.of(avis));
        when(avisEntrepriseRepository.saveAndFlush(avis)).thenReturn(avis);
        when(avisEntrepriseRepository.findByEntreprise_IdAndStatut(1L, StatutModeration.APPROUVE))
                .thenReturn(List.of(
                        avis(3L, entreprise, 4, StatutModeration.APPROUVE),
                        avis(4L, entreprise, 5, StatutModeration.APPROUVE)
                ));
        when(entrepriseRepository.findById(1L)).thenReturn(Optional.of(entreprise));
        when(entrepriseRepository.save(entreprise)).thenReturn(entreprise);

        AvisResponse response = avisService.moderate(2L, new ModerationRequest(StatutModeration.APPROUVE));

        assertEquals(StatutModeration.APPROUVE, response.statut());
        assertEquals(4.5, entreprise.getNoteMoyenne());
        verify(entrepriseRepository).save(entreprise);

        ArgumentCaptor<EvenementEntreprise> evenementCaptor = ArgumentCaptor.forClass(EvenementEntreprise.class);
        verify(applicationEventPublisher).publishEvent(evenementCaptor.capture());
        EvenementEntreprise evenement = evenementCaptor.getValue();
        assertEquals(EvenementEntreprise.AVIS_MODERE, evenement.type());
        assertNotNull(evenement.eventId());
        assertNotNull(evenement.date());
        assertTrue(evenement.donnees() instanceof EvenementEntreprise.AvisModere);
        EvenementEntreprise.AvisModere donnees = (EvenementEntreprise.AvisModere) evenement.donnees();
        assertEquals(2L, donnees.avisId());
        assertEquals(1L, donnees.entrepriseId());
        assertEquals(StatutModeration.APPROUVE, donnees.statut());
        assertEquals(4.5, donnees.noteMoyenne());
    }

    @Test
    void moderateRejectsAvisAndResetsAverageToZero() {
        Entreprise entreprise = entreprise(1L);
        AvisEntreprise avis = avis(2L, entreprise, 5, StatutModeration.APPROUVE);

        when(avisEntrepriseRepository.findById(2L)).thenReturn(Optional.of(avis));
        when(avisEntrepriseRepository.saveAndFlush(avis)).thenReturn(avis);
        when(avisEntrepriseRepository.findByEntreprise_IdAndStatut(1L, StatutModeration.APPROUVE)).thenReturn(List.of());
        when(entrepriseRepository.findById(1L)).thenReturn(Optional.of(entreprise));
        when(entrepriseRepository.save(entreprise)).thenReturn(entreprise);

        AvisResponse response = avisService.moderate(2L, new ModerationRequest(StatutModeration.REJETE));

        assertEquals(StatutModeration.REJETE, response.statut());
        assertEquals(0.0, entreprise.getNoteMoyenne());
        verify(applicationEventPublisher).publishEvent(any(EvenementEntreprise.class));
    }

    @Test
    void moderateDoesNotPublishEventWhenStatusIsUnchanged() {
        Entreprise entreprise = entreprise(1L);
        AvisEntreprise avis = avis(2L, entreprise, 5, StatutModeration.APPROUVE);

        when(avisEntrepriseRepository.findById(2L)).thenReturn(Optional.of(avis));
        when(avisEntrepriseRepository.saveAndFlush(avis)).thenReturn(avis);
        when(avisEntrepriseRepository.findByEntreprise_IdAndStatut(1L, StatutModeration.APPROUVE))
                .thenReturn(List.of(avis));
        when(entrepriseRepository.findById(1L)).thenReturn(Optional.of(entreprise));

        avisService.moderate(2L, new ModerationRequest(StatutModeration.APPROUVE));

        verify(applicationEventPublisher, never()).publishEvent(any(EvenementEntreprise.class));
    }

    @Test
    void moderateRejectsEnAttenteStatus() {
        Entreprise entreprise = entreprise(1L);
        AvisEntreprise avis = avis(2L, entreprise);

        when(avisEntrepriseRepository.findById(2L)).thenReturn(Optional.of(avis));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                avisService.moderate(2L, new ModerationRequest(StatutModeration.EN_ATTENTE))
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(avisEntrepriseRepository, never()).saveAndFlush(any(AvisEntreprise.class));
    }

    @Test
    void moderateRejectsUnknownAvis() {
        when(avisEntrepriseRepository.findById(2L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                avisService.moderate(2L, new ModerationRequest(StatutModeration.APPROUVE))
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void deleteAvisRecalculatesAverage() {
        Entreprise entreprise = entreprise(1L);
        AvisEntreprise avis = avis(2L, entreprise, 5, StatutModeration.APPROUVE);

        when(avisEntrepriseRepository.findById(2L)).thenReturn(Optional.of(avis));
        when(avisEntrepriseRepository.findByEntreprise_IdAndStatut(1L, StatutModeration.APPROUVE))
                .thenReturn(List.of(avis(3L, entreprise, 3, StatutModeration.APPROUVE)));
        when(entrepriseRepository.findById(1L)).thenReturn(Optional.of(entreprise));
        when(entrepriseRepository.save(entreprise)).thenReturn(entreprise);

        avisService.delete(2L);

        verify(avisEntrepriseRepository).delete(avis);
        verify(avisEntrepriseRepository).flush();
        assertEquals(3.0, entreprise.getNoteMoyenne());
    }

    @Test
    void deleteRejectsUnknownAvis() {
        when(avisEntrepriseRepository.findById(2L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                avisService.delete(2L)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    private Entreprise entreprise(Long id) {
        Entreprise entreprise = new Entreprise("TechNova", "Informatique", "Tunis", "Recrutement tech");
        entreprise.setId(id);
        return entreprise;
    }

    private UtilisateurInfo utilisateur(String role) {
        return new UtilisateurInfo(10L, "Alice Martin", "alice@example.com", role);
    }

    private AvisEntreprise avis(Long id, Entreprise entreprise) {
        return avis(id, entreprise, 5, StatutModeration.EN_ATTENTE);
    }

    private AvisEntreprise avis(Long id, Entreprise entreprise, int note, StatutModeration statut) {
        AvisEntreprise avis = new AvisEntreprise();
        avis.setId(id);
        avis.setEntreprise(entreprise);
        avis.setUtilisateurId(10L + id);
        avis.setNomUtilisateur("Alice Martin");
        avis.setNote(note);
        avis.setCommentaire("Super");
        avis.setStatut(statut);
        avis.setDateCreation(LocalDateTime.of(2026, 11, 22, 10, 0));
        return avis;
    }

    private AvisResponse response(AvisEntreprise avis) {
        return new AvisResponse(
                avis.getId(),
                avis.getEntreprise().getId(),
                avis.getUtilisateurId(),
                avis.getNomUtilisateur(),
                avis.getNote(),
                avis.getCommentaire(),
                avis.getStatut(),
                avis.getDateCreation()
        );
    }
}
