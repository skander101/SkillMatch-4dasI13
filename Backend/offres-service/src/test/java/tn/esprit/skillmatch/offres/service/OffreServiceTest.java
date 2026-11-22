package tn.esprit.skillmatch.offres.service; // Tests unitaires des règles métier.

import org.junit.jupiter.api.BeforeEach; // Préparation commune.
import org.junit.jupiter.api.Test; // Tests JUnit.
import org.junit.jupiter.api.extension.ExtendWith; // Active Mockito.
import org.junit.jupiter.params.ParameterizedTest; // Répète un test avec plusieurs statuts.
import org.junit.jupiter.params.provider.EnumSource; // Fournit les valeurs d'un enum.
import org.mockito.Mock; // Faux objets sans base ni réseau.
import org.mockito.junit.jupiter.MockitoExtension; // Intégration Mockito et JUnit.
import org.springframework.web.server.ResponseStatusException; // Erreurs attendues.
import org.springframework.context.ApplicationEventPublisher;
import tn.esprit.skillmatch.offres.event.EvenementOffres;
import tn.esprit.skillmatch.offres.dto.OffreRequest; // Formulaire testé.
import tn.esprit.skillmatch.offres.entity.Offre; // Offre testée.
import tn.esprit.skillmatch.offres.enums.*; // Contrats et statuts.
import tn.esprit.skillmatch.offres.repository.*; // Repositories simulés.
import tn.esprit.skillmatch.offres.security.IdentiteService; // Authentification simulée.
import tn.esprit.skillmatch.offres.security.IdentiteService.Identite; // Identité renvoyée par le serveur.
import java.time.LocalDate; // Dates des scénarios.
import java.util.List; // Compétences et résultats.
import java.util.Optional; // Offre présente ou absente.
import static org.junit.jupiter.api.Assertions.*; // Vérifications JUnit.
import static org.mockito.Mockito.*; // Simulation et vérification des appels.

@ExtendWith(MockitoExtension.class) // Les tests n'utilisent ni MySQL ni commun-service.
class OffreServiceTest { // Vérifie les règles indépendamment de HTTP.
    @Mock OffreRepository offres; // Faux accès aux offres.
    @Mock CandidatureRepository candidatures; // Faux accès aux candidatures.
    @Mock IdentiteService identites; // Fausse réponse d'identité fiable.
    @Mock ApplicationEventPublisher evenements;
    private OffreService service; // Vrai service métier à tester.
    private static final String TOKEN = "Bearer test"; // Valeur locale, pas un véritable JWT.

    @BeforeEach // Exécuté avant chaque scénario.
    void preparer() { // Recrée le service avec les faux objets.
        service = new OffreService(offres, candidatures, identites, evenements); // Aucun serveur démarré.
    } // Fin de la préparation.

    @Test // Lecture ADMIN.
    void adminVoitToutesLesOffres() { // Vérifie le périmètre global.
        connecter("ADMIN"); // Simule un administrateur connecté.
        service.lister(TOKEN, "", "titre"); // Demande la liste.
        verify(offres).findAll(); // Vérifie l'absence de filtre propriétaire.
    } // Fin du test.

    @Test // Lecture RECRUTEUR.
    void recruteurVoitSeulementSesOffres() { // Vérifie le filtre de propriétaire.
        connecter("RECRUTEUR"); // Utilisateur numéro 1.
        service.lister(TOKEN, "", "titre"); // Demande la liste.
        verify(offres).findByRecruteurId(1L); // Aucun identifiant librement choisi.
    } // Fin du test.

    @Test // Lecture CANDIDAT.
    void candidatVoitSeulementLesOffresOuvertes() { // Vérifie le filtre statut et date.
        connecter("CANDIDAT"); // Simule un candidat.
        service.lister(TOKEN, "", "titre"); // Consulte le catalogue.
        verify(offres).findByStatutAndDateExpirationGreaterThanEqual(OffreStatut.PUBLIEE, LocalDate.now()); // Inclut aujourd'hui.
        erreur(403, () -> service.mesOffres(TOKEN, "", "titre")); // Le candidat n'a pas d'offres à gérer.
    } // Fin du test.

    @ParameterizedTest // Répète la même règle pour les trois statuts fermés au candidat.
    @EnumSource(value = OffreStatut.class, names = {"BROUILLON", "FERMEE", "EXPIREE"}) // Statuts non publics.
    void candidatNeLitPasUneOffreNonPubliee(OffreStatut statut) { // Test de visibilité.
        connecter("CANDIDAT"); // Rôle candidat.
        Offre offre = existante(); // Prépare une offre en base simulée.
        offre.setStatut(statut); // Choisit le statut du scénario.
        erreur(403, () -> service.consulter(10L, TOKEN)); // La lecture est interdite.
    } // Fin du test paramétré.

    @Test // Date limite.
    void candidatNeLitPasUneOffrePublieeExpiree() { // Même si le statut n'a pas été changé automatiquement.
        connecter("CANDIDAT"); // Identité candidat.
        Offre offre = existante(); // Offre publiée par défaut dans ce test.
        offre.setDateExpiration(LocalDate.now().minusDays(1)); // Date dépassée.
        erreur(403, () -> service.consulter(10L, TOKEN)); // Refuse la lecture.
    } // Fin du test.

    @Test // Offre encore valide aujourd'hui.
    void candidatPeutLireUneOffreValideAujourdhui() { // Vérifie la limite de date.
        connecter("CANDIDAT"); // Identité candidat.
        Offre offre = existante(); // Offre publiée.
        offre.setDateExpiration(LocalDate.now()); // Dernier jour de validité.
        assertSame(offre, service.consulter(10L, TOKEN)); // La lecture est autorisée.
    } // Fin du test.

    @Test // Protection contre un autre recruteur.
    void recruteurNeConsulteNiNeModifieNiNeSupprimeUneOffreDautrui() { // Vérifie les trois opérations par identifiant.
        connecter("RECRUTEUR"); // Recruteur numéro 1.
        existante().setRecruteurId(2L); // L'offre appartient au recruteur numéro 2.
        erreur(403, () -> service.consulter(10L, TOKEN)); // Lecture refusée.
        erreur(403, () -> service.modifier(10L, demande(OffreStatut.PUBLIEE, null), TOKEN)); // Modification refusée.
        erreur(403, () -> service.supprimer(10L, TOKEN)); // Suppression refusée.
        verify(offres, never()).delete(any()); // Aucune suppression en base.
    } // Fin du test.

    @Test // Création classique.
    void creationRecruteurUtiliseSonIdentite() { // Vérifie le propriétaire et les données.
        connecter("RECRUTEUR"); // Session du recruteur 1.
        when(offres.save(any())).thenAnswer(appel -> appel.getArgument(0)); // Retourne l'offre enregistrée.
        Offre offre = service.creer(demande(OffreStatut.BROUILLON, null), TOKEN); // Aucun propriétaire envoyé.
        assertEquals(1L, offre.getRecruteurId()); // Propriétaire issu de la session.
        assertEquals("Java", offre.getTitre()); // Le titre a été nettoyé.
        assertEquals(OffreStatut.BROUILLON, offre.getStatut()); // Statut initial.
    } // Fin du test.

    @Test // Création par ADMIN.
    void adminCreePourUnRecruteurVerifie() { // Vérifie l'appel au contrôle d'identité.
        connecter("ADMIN"); // Session ADMIN.
        when(offres.save(any())).thenAnswer(appel -> appel.getArgument(0)); // Simule l'enregistrement.
        Offre offre = service.creer(demande(OffreStatut.BROUILLON, 7L), TOKEN); // Choisit le recruteur 7.
        verify(identites).verifierRecruteur(7L, TOKEN); // Contrôle serveur obligatoire.
        assertEquals(7L, offre.getRecruteurId()); // Conserve ce propriétaire.
    } // Fin du test.

    @Test // Référence ADMIN invalide.
    void adminNePeutPasInventerUnRecruteur() { // Simule une vérification distante refusée.
        connecter("ADMIN"); // Identité ADMIN.
        doThrow(new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST)) // Le serveur ne trouve pas ce recruteur.
                .when(identites).verifierRecruteur(99L, TOKEN); // Réponse simulée.
        erreur(400, () -> service.creer(demande(OffreStatut.BROUILLON, 99L), TOKEN)); // Création refusée.
        verify(offres, never()).save(any()); // Aucun enregistrement.
    } // Fin du test.

    @Test // Usurpation du propriétaire.
    void recruteurNePeutPasCreerAuNomDunAutre() { // Vérifie le formulaire.
        connecter("RECRUTEUR"); // Recruteur 1.
        erreur(403, () -> service.creer(demande(OffreStatut.BROUILLON, 7L), TOKEN)); // Recruteur 7 interdit.
        verify(offres, never()).save(any()); // Rien n'est enregistré.
    } // Fin du test.

    @Test // Candidat en lecture seule sur les offres.
    void candidatNePeutPasEcrire() { // Vérifie les trois écritures.
        connecter("CANDIDAT"); // Rôle non autorisé.
        erreur(403, () -> service.creer(demande(OffreStatut.BROUILLON, null), TOKEN)); // Création refusée.
        erreur(403, () -> service.modifier(10L, demande(OffreStatut.PUBLIEE, null), TOKEN)); // Modification refusée.
        erreur(403, () -> service.supprimer(10L, TOKEN)); // Suppression refusée.
        verifyNoInteractions(offres); // Aucun accès aux offres nécessaire.
    } // Fin du test.

    @Test // Création puis publication.
    void creationExigeUnBrouillon() { // Empêche une publication directe.
        connecter("RECRUTEUR"); // Gestionnaire autorisé.
        erreur(400, () -> service.creer(demande(OffreStatut.PUBLIEE, null), TOKEN)); // Statut initial invalide.
    } // Fin du test.

    @Test // Publication valide.
    void proprietairePeutPublierSonBrouillon() { // Vérifie la modification normale.
        connecter("RECRUTEUR"); // Propriétaire numéro 1.
        Offre offre = existante(); // Offre appartenant à 1.
        offre.setStatut(OffreStatut.BROUILLON); // État initial.
        when(offres.save(offre)).thenReturn(offre); // Simule l'enregistrement.
        assertEquals(OffreStatut.PUBLIEE, service.modifier(10L, demande(OffreStatut.PUBLIEE, null), TOKEN).getStatut()); // Publication réussie.
    } // Fin du test.

    @Test // ADMIN peut gérer une offre sans en être propriétaire.
    void adminPeutModifierUneOffreDautrui() { // Vérifie l'exception administrative.
        connecter("ADMIN"); // Administrateur 1.
        Offre offre = existante(); // Offre existante.
        offre.setRecruteurId(7L); // Propriétaire différent.
        when(offres.save(offre)).thenReturn(offre); // Enregistrement simulé.
        service.modifier(10L, demande(OffreStatut.FERMEE, null), TOKEN); // Ferme l'offre.
        assertEquals(OffreStatut.FERMEE, offre.getStatut()); // Modification autorisée.
        assertEquals(7L, offre.getRecruteurId()); // Propriétaire conservé.
    } // Fin du test.

    @Test // Propriété immuable.
    void modificationNeTransferePasLaPropriete() { // Même ADMIN doit garder le propriétaire.
        connecter("ADMIN"); // Identité ADMIN.
        existante(); // Offre appartenant au recruteur 1.
        erreur(400, () -> service.modifier(10L, demande(OffreStatut.PUBLIEE, 7L), TOKEN)); // Changement refusé.
    } // Fin du test.

    @Test // Protection des candidatures.
    void suppressionAvecCandidatureRetourne409() { // Empêche la perte de candidatures.
        connecter("RECRUTEUR"); // Propriétaire.
        existante(); // Offre existante.
        when(candidatures.existsByOffreId(10L)).thenReturn(true); // Une candidature existe.
        erreur(409, () -> service.supprimer(10L, TOKEN)); // Conflit métier.
        verify(offres, never()).delete(any()); // L'offre reste conservée.
    } // Fin du test.

    @Test // Suppression autorisée.
    void adminSupprimeUneOffreSansCandidature() { // Vérifie le CRUD global.
        connecter("ADMIN"); // Administrateur connecté.
        Offre offre = existante(); // Offre existante.
        offre.setRecruteurId(7L); // Autre propriétaire.
        service.supprimer(10L, TOKEN); // Aucune candidature par défaut dans le mock.
        verify(offres).delete(offre); // Suppression appelée.
        verify(offres).flush(); // Conflit SQL éventuel détecté immédiatement.
    } // Fin du test.

    @Test // Identifiant inconnu.
    void offreAbsenteRetourne404() { // Vérifie la réponse commune aux opérations.
        connecter("ADMIN"); // Utilisateur autorisé.
        erreur(404, () -> service.consulter(99L, TOKEN)); // Optional vide par défaut.
    } // Fin du test.

    @Test // Cohérence des dates.
    void publicationPasseeEtExpirationFutureSontRefusees() { // Vérifie les deux sens.
        connecter("RECRUTEUR"); // Propriétaire autorisé.
        existante(); // Offre modifiable.
        OffreRequest base = demande(OffreStatut.PUBLIEE, null); // Demande valide à adapter.
        OffreRequest passee = new OffreRequest(base.titre(), null, base.competences(), null, base.contrat(), null, LocalDate.now().minusDays(1), base.statut(), null, null, null); // Publication périmée.
        erreur(400, () -> service.modifier(10L, passee, TOKEN)); // Publication refusée.
        erreur(400, () -> service.modifier(10L, demande(OffreStatut.EXPIREE, null), TOKEN)); // Date future incompatible avec EXPIREE.
    } // Fin du test.

    @Test // Fermeture d'une offre périmée.
    void peutFermerUneOffreDontLaDateEstPassee() { // La conservation d'un historique reste possible.
        connecter("RECRUTEUR"); // Propriétaire.
        Offre offre = existante(); // Offre à fermer.
        OffreRequest base = demande(OffreStatut.FERMEE, null); // Formulaire de fermeture.
        OffreRequest fermee = new OffreRequest(base.titre(), null, base.competences(), null, base.contrat(), null, LocalDate.now().minusDays(1), base.statut(), null, null, null); // Date déjà passée.
        when(offres.save(offre)).thenReturn(offre); // Enregistrement simulé.
        assertEquals(OffreStatut.FERMEE, service.modifier(10L, fermee, TOKEN).getStatut()); // Fermeture autorisée.
    } // Fin du test.

    @Test // Recherche simple par titre.
    void rechercheIgnoreLaCasseEtLesEspaces() { // Aucun filtre supplémentaire.
        connecter("RECRUTEUR"); // Propriétaire connecté.
        Offre java = offreDeListe("Développeur Java", 2000.0); // Titre correspondant.
        Offre angular = offreDeListe("Développeur Angular", 3000.0); // Autre titre.
        when(offres.findByRecruteurId(1L)).thenReturn(List.of(java, angular)); // Seulement ses offres.
        assertEquals(List.of(java), service.lister(TOKEN, " JAVA ", "titre")); // Recherche insensible à la casse.
        assertTrue(service.mesOffres(TOKEN, "Python", "titre").isEmpty()); // Aucun titre correspondant.
    } // Fin du test.

    @Test // Vérifie les trois tris.
    void trieParTitreSalaireOuExpiration() { // Deux offres suffisent.
        connecter("ADMIN"); // Toutes les offres sont accessibles.
        Offre z = offreDeListe("Zebra", 1000.0); // Titre après Alpha, mais salaire plus petit.
        Offre a = offreDeListe("Alpha", null); // Salaire absent.
        z.setDateExpiration(LocalDate.now().plusDays(1)); // Expiration plus proche.
        when(offres.findAll()).thenReturn(List.of(z, a)); // Ordre de départ.
        assertEquals(List.of(a, z), service.lister(TOKEN, "", "titre")); // A-Z.
        assertEquals(List.of(z, a), service.lister(TOKEN, "", "salaire")); // Salaire absent à la fin.
        assertEquals(List.of(z, a), service.lister(TOKEN, "", "dateExpiration")); // Date la plus proche.
        erreur(400, () -> service.lister(TOKEN, "", "inconnu")); // Tri invalide.
    } // Fin du test.

    @Test // Vérifie les trois tris décroissants.
    void trieDansLordreDecroissant() { // Garde les salaires absents à la fin.
        connecter("ADMIN"); // Identité autorisée.
        Offre a = offreDeListe("Alpha", 1000.0); // Petit salaire et titre A.
        Offre z = offreDeListe("Zebra", 3000.0); // Grand salaire et titre Z.
        Offre sansSalaire = offreDeListe("Milieu", null); // Salaire non renseigné.
        z.setDateExpiration(LocalDate.now().plusDays(20)); // Date la plus lointaine.
        a.setDateExpiration(LocalDate.now().plusDays(1)); // Date la plus proche.
        when(offres.findAll()).thenReturn(List.of(a, z, sansSalaire)); // Liste de départ.
        assertEquals(List.of(z, sansSalaire, a), service.lister(TOKEN, "", "titreDesc")); // Z-A.
        assertEquals(List.of(z, a, sansSalaire), service.lister(TOKEN, "", "salaireDesc")); // Grand au petit, puis absence.
        assertEquals(List.of(z, sansSalaire, a), service.lister(TOKEN, "", "dateExpirationDesc")); // Date lointaine d'abord.
    } // Fin du test.

    @Test
    void publieUnEvenementSeulementLorsDuPassageAPubliee() {
        connecter("RECRUTEUR");
        Offre offre = existante();
        offre.setStatut(OffreStatut.BROUILLON);
        service.modifier(10L, demande(OffreStatut.PUBLIEE, null), TOKEN);
        verify(evenements).publishEvent(argThat((Object e) -> e instanceof EvenementOffres v && v.type().equals("OfferPublished")));
        clearInvocations(evenements);
        service.modifier(10L, demande(OffreStatut.PUBLIEE, null), TOKEN); // Même statut : pas de deuxième publication.
        service.modifier(10L, demande(OffreStatut.FERMEE, null), TOKEN);
        verifyNoInteractions(evenements);
    }

    private Offre offreDeListe(String titre, Double salaire) { // Prépare une offre pour la recherche.
        Offre offre = new Offre(); // Objet de test.
        offre.setTitre(titre); // Titre recherché.
        offre.setSalaire(salaire); // Salaire utilisé pour le tri.
        offre.setDateExpiration(LocalDate.now().plusDays(10)); // Date utilisée pour le tri.
        return offre; // Retourne l'objet.
    } // Fin du helper.

    private void connecter(String role) { // Prépare seulement le rôle utile au scénario.
        when(identites.courante(TOKEN)).thenReturn(new Identite(1L, "Test", "test@example.invalid", role)); // Identité de test.
    } // Fin de la préparation d'identité.

    private Offre existante() { // Offre standard commune aux scénarios.
        Offre offre = new Offre(); // Crée l'objet.
        offre.setId(10L); // Identifiant d'offre.
        offre.setRecruteurId(1L); // Propriétaire 1.
        offre.setStatut(OffreStatut.PUBLIEE); // Offre publique.
        offre.setDateExpiration(LocalDate.now().plusDays(5)); // Date valide.
        when(offres.findById(10L)).thenReturn(Optional.of(offre)); // Simule sa présence.
        return offre; // Permet d'adapter l'objet au test.
    } // Fin de la préparation.

    private OffreRequest demande(OffreStatut statut, Long proprietaire) { // Formulaire valide standard.
        return new OffreRequest(" Java ", "Description", List.of("Java"), "Tunis", ContratType.CDI, 2000.0, LocalDate.now().plusDays(5), statut, null, null, proprietaire); // Aucun utilisateur réel créé.
    } // Fin du formulaire.

    private void erreur(int code, Runnable operation) { // Évite de répéter les assertions d'erreur.
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, operation::run); // Exécute l'opération attendue en échec.
        assertEquals(code, exception.getStatusCode().value()); // Vérifie le code exact.
    } // Fin du helper de vérification.
} // Fin des tests métier.
