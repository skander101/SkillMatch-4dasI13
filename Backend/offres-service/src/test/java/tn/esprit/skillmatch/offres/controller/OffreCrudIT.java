package tn.esprit.skillmatch.offres.controller; // Test d'intégration du CRUD avec MySQL.

import org.junit.jupiter.api.Test; // Déclare un test.
import org.springframework.beans.factory.annotation.Autowired; // Injecte les vrais composants.
import org.springframework.boot.test.context.SpringBootTest; // Charge l'application.
import org.springframework.boot.test.mock.mockito.MockBean; // Simule le service d'identité.
import org.springframework.transaction.annotation.Transactional; // Annule les données à la fin.
import tn.esprit.skillmatch.offres.dto.OffreRequest; // Formulaire d'une offre.
import tn.esprit.skillmatch.offres.entity.Offre; // Entité enregistrée.
import tn.esprit.skillmatch.offres.enums.*; // Contrat et statut.
import tn.esprit.skillmatch.offres.repository.OffreRepository; // Vérifie les données MySQL.
import tn.esprit.skillmatch.offres.security.IdentiteService; // Identité distante simulée.
import tn.esprit.skillmatch.offres.security.IdentiteService.Identite; // Utilisateur de test.
import tn.esprit.skillmatch.offres.service.OffreService; // Vrai service métier.
import java.time.LocalDate; // Date d'expiration.
import java.util.List; // Compétences et résultats.
import static org.junit.jupiter.api.Assertions.*; // Vérifications du test.
import static org.mockito.Mockito.when; // Prépare la réponse d'identité.

// Lancer avec MySQL : mvn "-Dtest=OffreCrudIT" test.
// Les routes HTTP sont testées séparément dans OffreControllerTest.
@SpringBootTest(properties = "eureka.client.enabled=false") // Utilise le vrai service et la vraie base.
@Transactional // Aucun enregistrement de test n'est conservé.
class OffreCrudIT { // Un seul scénario, facile à suivre.
    @Autowired OffreService service; // Exécute le CRUD.
    @Autowired OffreRepository offres; // Vérifie la base.
    @MockBean IdentiteService identites; // Ne teste pas de vraies signatures JWT.
    private final String token = "Bearer test"; // Jeton fictif, réservé au test.

    @Test // Vérifie les quatre opérations dans l'ordre.
    void verifierCrud() { // Créer, lire, modifier, supprimer.
        when(identites.courante(token)).thenReturn(new Identite(1L, "Test", "test@example.invalid", "RECRUTEUR")); // Simule le recruteur connecté.
        Offre offre = service.creer(formulaire("Java", OffreStatut.BROUILLON), token); // CREATE : enregistre un brouillon.
        Long id = offre.getId(); // Récupère l'identifiant généré.
        assertTrue(offres.existsById(id)); // Vérifie que l'offre existe dans MySQL.
        assertEquals("Entreprise de test", service.consulter(id, token).getNomEntreprise());
        assertEquals("Java", service.consulter(id, token).getTitre()); // READ : vérifie le titre.
        service.modifier(id, formulaire("Angular", OffreStatut.PUBLIEE), token); // UPDATE : change le titre et publie.
        offres.flush(); offres.findById(id).orElseThrow();
        assertEquals("Entreprise de test", service.consulter(id, token).getNomEntreprise());
        assertEquals("Angular", service.consulter(id, token).getTitre()); // Vérifie le nouveau titre.
        assertEquals(OffreStatut.PUBLIEE, service.consulter(id, token).getStatut()); // Vérifie la publication.
        assertEquals(List.of(offre), service.lister(token, "angular", "titre")); // Vérifie aussi la recherche simple.
        service.supprimer(id, token); // DELETE : supprime l'offre sans candidature.
        assertFalse(offres.existsById(id)); // Vérifie la suppression dans MySQL.
    } // Fin du scénario.

    private OffreRequest formulaire(String titre, OffreStatut statut) { // Prépare seulement les données nécessaires.
        return new OffreRequest(titre, "Description détaillée du poste Java", List.of("Java"), "Tunis", ContratType.CDI, 2000.0, // Champs simples.
                LocalDate.now().plusDays(10), statut, 1L, 1L, null, "Entreprise de test"); // Date valide et références obligatoires.
    } // Fin du formulaire.
} // Fin du fichier.
