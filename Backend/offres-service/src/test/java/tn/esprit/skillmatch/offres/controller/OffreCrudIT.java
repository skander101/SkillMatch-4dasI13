package tn.esprit.skillmatch.offres.controller; // Tests du CRUD HTTP avec la vraie base MySQL.

import com.fasterxml.jackson.databind.ObjectMapper; // Lecture et écriture JSON.
import org.junit.jupiter.api.Test; // Tests JUnit.
import org.springframework.beans.factory.annotation.Autowired; // Injection des vrais composants.
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc; // Active les appels HTTP de test.
import org.springframework.boot.test.context.SpringBootTest; // Charge l'application complète.
import org.springframework.boot.test.mock.mockito.MockBean; // Remplace seulement l'identité distante.
import org.springframework.http.MediaType; // Type JSON.
import org.springframework.test.web.servlet.MockMvc; // Exécute les routes sans ouvrir de port.
import org.springframework.transaction.annotation.Transactional; // Annule les données à la fin de chaque test.
import tn.esprit.skillmatch.offres.dto.OffreRequest; // Formulaire réel.
import tn.esprit.skillmatch.offres.entity.*; // Entités persistées.
import tn.esprit.skillmatch.offres.enums.*; // Contrats et statuts.
import tn.esprit.skillmatch.offres.repository.*; // Vrais repositories JPA.
import tn.esprit.skillmatch.offres.security.IdentiteService; // Seul composant simulé.
import tn.esprit.skillmatch.offres.security.IdentiteService.Identite; // Identité de test.
import java.time.LocalDate; // Dates des offres.
import java.util.List; // Compétences.
import static org.junit.jupiter.api.Assertions.*; // Vérifications sur la base.
import static org.mockito.Mockito.*; // Préparation de l'identité simulée.
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*; // Requêtes HTTP.
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*; // Statuts et réponses attendus.

// Commande manuelle : mvn "-Dtest=OffreCrudIT" test ; MySQL doit être disponible.
// L'identité est simulée : ce test ne prétend pas vérifier de vraies signatures JWT.
@SpringBootTest // Charge les vrais contrôleurs, services et repositories.
@AutoConfigureMockMvc // Prépare les routes HTTP.
@Transactional // Les INSERT, UPDATE et DELETE sont annulés après chaque test.
class OffreCrudIT { // Vérifie HTTP, règles métier et persistance ensemble.
    @Autowired MockMvc mvc; // Client de test.
    @Autowired ObjectMapper json; // Convertisseur JSON de l'application.
    @Autowired OffreRepository offres; // Accès réel à offres_db.
    @Autowired CandidatureRepository candidatures; // Accès réel aux candidatures.
    @MockBean IdentiteService identites; // Aucun appel à commun-service ni compte réel.

    @Test // Cycle complet d'une offre.
    void recruteurCreeLitPubliePuisSupprimeSonOffre() throws Exception { // Vérifie les quatre opérations CRUD.
        connecter("RECRUTEUR"); // Identité fictive validée pour ce scénario.
        String resultat = mvc.perform(post("/skillmatch/offres").header("Authorization", "Bearer test") // Crée un brouillon.
                .contentType(MediaType.APPLICATION_JSON).content(formulaire(OffreStatut.BROUILLON, null))) // Formulaire valide.
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(); // Attend 201.
        long id = json.readTree(resultat).get("id").asLong(); // Récupère l'identifiant réellement généré.
        assertTrue(offres.existsById(id)); // Vérifie l'INSERT dans MySQL.
        mvc.perform(get("/skillmatch/offres/{id}", id).header("Authorization", "Bearer test")) // Relit l'offre.
                .andExpect(status().isOk()).andExpect(jsonPath("$.competences[0]").value("Java")); // JSON relu depuis TEXT.
        mvc.perform(get("/skillmatch/offres/mes-offres").header("Authorization", "Bearer test")) // Liste personnelle.
                .andExpect(status().isOk()); // Route accessible au propriétaire.
        mvc.perform(put("/skillmatch/offres/{id}", id).header("Authorization", "Bearer test") // Publie l'offre.
                .contentType(MediaType.APPLICATION_JSON).content(formulaire(OffreStatut.PUBLIEE, null))) // Même propriétaire.
                .andExpect(status().isOk()).andExpect(jsonPath("$.statut").value("PUBLIEE")); // UPDATE confirmé.
        connecter("CANDIDAT"); // Passe à la lecture candidat.
        mvc.perform(get("/skillmatch/offres/{id}", id).header("Authorization", "Bearer test")) // Offre ouverte.
                .andExpect(status().isOk()); // Lecture autorisée.
        connecter("RECRUTEUR"); // Reprend la session du propriétaire.
        mvc.perform(delete("/skillmatch/offres/{id}", id).header("Authorization", "Bearer test")) // Aucune candidature.
                .andExpect(status().isNoContent()); // DELETE autorisé.
        assertFalse(offres.existsById(id)); // Vérifie la suppression réelle.
    } // Fin du cycle CRUD.

    @Test // Protection des candidatures.
    void refuseSuppressionAvecCandidatureMaisAutoriseFermeture() throws Exception { // Règle de conservation.
        connecter("RECRUTEUR"); // Propriétaire numéro 1.
        Offre offre = creerOffre(); // Insère une offre valide.
        Candidature candidature = new Candidature(); // Prépare une candidature.
        candidature.setOffre(offre); // Référence l'offre.
        candidature.setUserId(2L); // Identifiant de test uniquement.
        candidature.setCvId(3L); // Référence CV de test uniquement.
        candidatures.saveAndFlush(candidature); // INSERT réel.
        mvc.perform(delete("/skillmatch/offres/{id}", offre.getId()).header("Authorization", "Bearer test")) // Demande de suppression.
                .andExpect(status().isConflict()); // Réponse métier 409.
        assertTrue(candidatures.existsById(candidature.getId())); // La candidature est conservée.
        mvc.perform(put("/skillmatch/offres/{id}", offre.getId()).header("Authorization", "Bearer test") // Alternative autorisée.
                .contentType(MediaType.APPLICATION_JSON).content(formulaire(OffreStatut.FERMEE, null))) // Ferme l'offre.
                .andExpect(status().isOk()).andExpect(jsonPath("$.statut").value("FERMEE")); // Offre conservée.
    } // Fin du test d'intégrité.

    @Test // Création administrative.
    void adminPeutCreerEtSupprimerPourUnRecruteur() throws Exception { // Vérifie le CRUD global.
        connecter("ADMIN"); // Identité ADMIN simulée.
        String resultat = mvc.perform(post("/skillmatch/offres").header("Authorization", "Bearer test") // Création ADMIN.
                .contentType(MediaType.APPLICATION_JSON).content(formulaire(OffreStatut.BROUILLON, 7L))) // Recruteur choisi.
                .andExpect(status().isCreated()).andExpect(jsonPath("$.recruteurId").value(7)) // Bonne association.
                .andReturn().getResponse().getContentAsString(); // Réponse JSON.
        verify(identites).verifierRecruteur(7L, "Bearer test"); // Vérification du recruteur exigée par le vrai service.
        long id = json.readTree(resultat).get("id").asLong(); // Identifiant généré.
        mvc.perform(delete("/skillmatch/offres/{id}", id).header("Authorization", "Bearer test")) // Offre d'un autre propriétaire.
                .andExpect(status().isNoContent()); // ADMIN est autorisé.
    } // Fin du test ADMIN.

    private void connecter(String role) { // Prépare l'identité du scénario.
        when(identites.courante("Bearer test")).thenReturn(new Identite(1L, "Test", "test@example.invalid", role)); // Aucun JWT réel utilisé.
    } // Fin de la préparation.

    private String formulaire(OffreStatut statut, Long recruteur) throws Exception { // Formulaire valide standard.
        return json.writeValueAsString(new OffreRequest("Offre CRUD transactionnelle", "Description", List.of("Java"), "Tunis", ContratType.CDI, 2000.0, LocalDate.now().plusDays(10), statut, null, null, recruteur)); // Données annulées en fin de test.
    } // Fin du formulaire.

    private Offre creerOffre() { // Offre utilisée pour le scénario avec candidature.
        Offre offre = new Offre(); // Nouvel objet.
        offre.setTitre("Offre avec candidature de test"); // Titre obligatoire.
        offre.setContrat(ContratType.CDI); // Contrat obligatoire.
        offre.setDateExpiration(LocalDate.now().plusDays(10)); // Date valide.
        offre.setRecruteurId(1L); // Même propriétaire que la session.
        return offres.saveAndFlush(offre); // Enregistre dans la transaction de test.
    } // Fin de la création de test.
} // Fin des tests CRUD MySQL.
