package tn.esprit.skillmatch.offres.service; // Tests courts des règles essentielles.

import org.junit.jupiter.api.*; // Tests et préparation.
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith; // Active Mockito.
import org.mockito.*; // Faux repositories.
import org.mockito.junit.jupiter.MockitoExtension; // Support Mockito.
import org.springframework.web.server.ResponseStatusException; // Erreurs attendues.
import org.springframework.context.ApplicationEventPublisher;
import tn.esprit.skillmatch.offres.event.EvenementOffres;
import tn.esprit.skillmatch.offres.dto.*; // Formulaires.
import tn.esprit.skillmatch.offres.entity.*; // Entités.
import tn.esprit.skillmatch.offres.enums.*; // Statuts.
import tn.esprit.skillmatch.offres.repository.*; // Repositories simulés.
import tn.esprit.skillmatch.offres.security.IdentiteService; // Identité simulée.
import tn.esprit.skillmatch.offres.security.IdentiteService.Identite;
import java.time.LocalDate; // Date de l'offre.
import java.time.LocalDateTime; // Dates simulées pour vérifier les deux sens de tri.
import java.util.List; // Liste des candidatures.
import java.util.Optional; // Résultats des recherches.
import static org.junit.jupiter.api.Assertions.*; // Vérifications.
import static org.mockito.Mockito.*; // Simulation.

@ExtendWith(MockitoExtension.class)
class CandidatureServiceTest {
    @Mock CandidatureRepository candidatures;
    @Mock OffreRepository offres;
    @Mock IdentiteService identites;
    @Mock ApplicationEventPublisher evenements;
    CandidatureService service;
    Offre offre;
    Candidature candidature;
    final String token = "Bearer test";

    @BeforeEach
    void preparer() { // Jeu de données local, sans MySQL.
        service = new CandidatureService(candidatures, offres, identites, evenements, new tn.esprit.skillmatch.offres.service.OffreService(null, null, null, null));
        offre = new Offre();
        offre.setId(10L);
        offre.setRecruteurId(2L);
        offre.setStatut(OffreStatut.PUBLIEE);
        offre.setDateExpiration(LocalDate.now().plusDays(10));
        candidature = new Candidature();
        candidature.setOffre(offre);
        candidature.setUserId(1L);
        candidature.setCvId(3L);
        candidature.setLettre("Je suis motivé pour rejoindre votre équipe Java.");
        candidature.setDateEntretien(LocalDateTime.now().plusDays(1));
        candidature.setNoteRecruteur(8);
        candidature.setCommentaireRecruteur("Profil intéressant");
    }

    @Test
    void creationRefuseUsurpationOffreFermeeEtDoublon() {
        connecter("CANDIDAT", 1L);
        erreur(403, () -> service.creer(new CandidatureRequest(10L, "Lettre", 3L, 9L), token)); // Faux propriétaire.
        when(offres.findById(10L)).thenReturn(Optional.of(offre));
        offre.setStatut(OffreStatut.FERMEE);
        erreur(409, () -> service.creer(demande(), token)); // Offre fermée.
        offre.setStatut(OffreStatut.PUBLIEE);
        offre.setDateExpiration(LocalDate.now().minusDays(1));
        erreur(409, () -> service.creer(demande(), token)); // Offre expirée.
        offre.setDateExpiration(LocalDate.now().plusDays(10));
        when(candidatures.existsByUserIdAndOffreId(1L, 10L)).thenReturn(true);
        erreur(409, () -> service.creer(demande(), token)); // Déjà postulé.
    }

    @Test
    void candidatNePeutPasSeNoterNiModifierApresTraitement() {
        connecter("CANDIDAT", 1L);
        when(candidatures.findById(5L)).thenReturn(Optional.of(candidature));
        erreur(403, () -> service.modifier(5L, new CandidatureUpdate(null, null, CandidatureStatut.ACCEPTEE, null, 20, null), token));
        candidature.setStatut(CandidatureStatut.PRESELECTIONNEE);
        erreur(409, () -> service.modifier(5L, new CandidatureUpdate("Lettre", 3L, null, null, null, null), token));
        erreur(409, () -> service.supprimer(5L, token)); // Retrait trop tardif.
    }

    @Test
    void autreCandidatEtAutreRecruteurSontRefuses() {
        when(candidatures.findById(5L)).thenReturn(Optional.of(candidature));
        connecter("CANDIDAT", 9L);
        erreur(403, () -> service.consulter(5L, token));
        connecter("RECRUTEUR", 9L);
        erreur(403, () -> service.consulter(5L, token));
        connecter("RECRUTEUR", 2L);
        erreur(403, () -> service.supprimer(5L, token)); // Même le propriétaire de l'offre ne peut pas supprimer.
        erreur(403, () -> service.creer(demande(), token)); // Il ne peut pas postuler non plus.
        candidature.setDateEntretien(null);
        erreur(400, () -> service.modifier(5L, new CandidatureUpdate(null, null, CandidatureStatut.ENTRETIEN_PLANIFIE, null, null, null), token));
        candidature.setDateEntretien(LocalDateTime.now().plusDays(1));
        when(candidatures.save(candidature)).thenReturn(candidature);
        assertEquals(8, service.modifier(5L, new CandidatureUpdate(null, null, CandidatureStatut.PRESELECTIONNEE, null, 8, "Avis favorable"), token).noteRecruteur()); // Traitement autorisé.
        candidature.setStatut(CandidatureStatut.RETIREE);
        erreur(409, () -> service.modifier(5L, new CandidatureUpdate(null, null, CandidatureStatut.ACCEPTEE, null, null, null), token)); // Retrait respecté.
    }

    @Test
    void adminVerifieLeCandidatAvantDeCreer() {
        connecter("ADMIN", 9L);
        when(offres.findById(10L)).thenReturn(Optional.of(offre));
        when(candidatures.saveAndFlush(any())).thenAnswer(appel -> appel.getArgument(0));
        assertEquals(7L, service.creer(new CandidatureRequest(10L, "Lettre", 3L, 7L), token).userId());
        verify(identites).verifierCandidat(7L, token); // Vérification serveur exigée.
        verify(evenements).publishEvent(argThat((Object e) -> e instanceof EvenementOffres v && v.type().equals("ApplicationSubmitted")));
    }

    @Test
    void rechercheEtTrieSansChangerLeProprietaire() {
        connecter("CANDIDAT", 1L);
        Candidature ancienne = candidatureDatee(1L, LocalDateTime.of(2026, 1, 1, 10, 0));
        Candidature recente = candidatureDatee(2L, LocalDateTime.of(2026, 2, 1, 10, 0));
        when(candidatures.findByUserId(1L)).thenReturn(List.of(recente, ancienne)); // Déjà limité à l'utilisateur.
        assertEquals(1L, service.lister(token, " JAVA ", "dateCandidature").get(0).id()); // Ancienne d'abord.
        assertEquals(2L, service.lister(token, "java", "dateCandidatureDesc").get(0).id()); // Récente d'abord.
        assertTrue(service.lister(token, "Python", "dateCandidatureDesc").isEmpty()); // Aucun résultat.
        erreur(400, () -> service.lister(token, "", "inconnu")); // Tri refusé.
    }

    private Candidature candidatureDatee(Long id, LocalDateTime date) { // La vraie date est automatique en base.
        Candidature c = mock(Candidature.class);
        when(c.getDateCandidature()).thenReturn(date);
        when(c.getId()).thenReturn(id);
        when(c.getLettre()).thenReturn("Motivation Java");
        when(c.getOffre()).thenReturn(offre);
        return c;
    }

    private CandidatureRequest demande() { return new CandidatureRequest(10L, "Lettre", 3L, null); } // Candidat de session.

    @ParameterizedTest
    @ValueSource(strings = {"CANDIDAT", "ADMIN"})
    void refuseUneLettreVideLorsDeLaModification(String role) {
        connecter(role, 1L);
        when(candidatures.findById(5L)).thenReturn(Optional.of(candidature));
        erreur(400, () -> service.modifier(5L, new CandidatureUpdate(" \n ", 1L, null, null, null, null), token));
        verify(candidatures, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"RECRUTEUR", "ADMIN"})
    void refuseUnAncienEntretienMemeInchange(String role) {
        connecter(role, 2L);
        candidature.setStatut(CandidatureStatut.ENTRETIEN_PLANIFIE);
        candidature.setDateEntretien(LocalDateTime.now().minusDays(1));
        when(candidatures.findById(5L)).thenReturn(Optional.of(candidature));
        erreur(400, () -> service.modifier(5L, new CandidatureUpdate(null, null, null, null, null, "Avis favorable"), token));
        verify(candidatures, never()).save(any());
    }

    @Test
    void signaleLeChangementDeStatutEtLeRetraitMaisPasUnStatutInchange() {
        connecter("RECRUTEUR", 2L);
        when(candidatures.findById(5L)).thenReturn(Optional.of(candidature));
        when(candidatures.save(candidature)).thenReturn(candidature);
        var demande = new CandidatureUpdate(null, null, CandidatureStatut.PRESELECTIONNEE, null, null, null);
        service.modifier(5L, demande, token);
        ArgumentCaptor<EvenementOffres> capture = ArgumentCaptor.forClass(EvenementOffres.class);
        verify(evenements).publishEvent(capture.capture());
        assertEquals("RECUE", ((EvenementOffres.StatutChange) capture.getValue().donnees()).ancienStatut());
        clearInvocations(evenements);
        service.modifier(5L, demande, token);
        verifyNoInteractions(evenements);
        connecter("CANDIDAT", 1L);
        candidature.setStatut(CandidatureStatut.RECUE);
        service.supprimer(5L, token);
        verify(evenements).publishEvent(capture.capture());
        assertEquals("RETIREE", ((EvenementOffres.StatutChange) capture.getValue().donnees()).nouveauStatut());
    }

    private void connecter(String role, Long id) { when(identites.courante(token)).thenReturn(new Identite(id, "Test", "test@example.invalid", role)); } // Identité de test.

    private void erreur(int code, Runnable action) {
        assertEquals(code, assertThrows(ResponseStatusException.class, action::run).getStatusCode().value()); // Vérifie le refus exact.
    }
}
