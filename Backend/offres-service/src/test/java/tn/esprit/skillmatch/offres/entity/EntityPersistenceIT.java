package tn.esprit.skillmatch.offres.entity; // Place ce fichier dans le package indiqué, qui organise les classes du service.

import jakarta.persistence.PersistenceException; // Importe l'erreur JPA attendue si une contrainte de base est violée.
import org.junit.jupiter.api.Test; // Importe l'annotation qui désigne un test JUnit.
import org.springframework.beans.factory.annotation.Autowired; // Importe l'injection automatique d'un objet par Spring.
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase; // Importe le choix de la base utilisée par le test.
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest; // Importe le test de la couche JPA avec transactions annulées à la fin.
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager; // Importe l'outil de test pour enregistrer et relire les entités.
import tn.esprit.skillmatch.offres.enums.ContratType; // Importe les valeurs autorisées de ContratType.
import tn.esprit.skillmatch.offres.enums.CandidatureStatut; // Importe les valeurs autorisées de CandidatureStatut.

import java.time.LocalDate; // Importe les dates sans heure.
import java.util.List; // Importe le type liste.

import static org.junit.jupiter.api.Assertions.*; // Importe les vérifications JUnit comme assertEquals et assertTrue.

// Test manuel avec MySQL/XAMPP : mvn -Dtest=EntityPersistenceIT test.
// Les donnees sont annulees en fin de test ; les identifiants peuvent avancer.
@DataJpaTest // Charge la couche JPA ; chaque test s'exécute dans une transaction annulée à la fin.
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // Garde la vraie base configurée au lieu de la remplacer par une base en mémoire.
class EntityPersistenceIT { // Regroupe les tests de EntityPersistenceIT.
    @Autowired // Demande à Spring de fournir automatiquement l'objet suivant.
    private TestEntityManager em; // Déclare l'outil qui enregistre et relit les entités pendant le test.

    @Test // Indique que JUnit doit exécuter cette méthode comme un test.
    void enregistreLesCompetencesEtLaRelationOffreCandidature() { // Vérifie l'enregistrement et la relecture des deux entités dans MySQL.
        Offre offre = creerOffre(); // Crée et enregistre une offre de test dans la transaction.
        Candidature candidature = creerCandidature(offre); // Enregistre une candidature rattachée à cette offre.
        em.clear(); // Vide le cache JPA pour obliger les lectures suivantes à interroger la base.

        Offre relue = em.find(Offre.class, offre.getId()); // Relit l'offre depuis la base grâce à son identifiant.
        Candidature recue = em.find(Candidature.class, candidature.getId()); // Relit la candidature depuis la base.
        assertEquals(List.of("Java", "SQL"), relue.getCompetences()); // Vérifie que les compétences ont été correctement enregistrées et relues.
        assertEquals(offre.getId(), recue.getOffre().getId()); // Vérifie que la candidature référence la bonne offre.
        assertEquals(CandidatureStatut.RECUE, recue.getStatut()); // Vérifie le statut initial réellement enregistré en base.
        assertNotNull(recue.getDateCandidature()); // Vérifie que JPA a renseigné automatiquement la date.

        em.remove(recue); // Demande la suppression de la candidature de test.
        em.flush(); // Envoie immédiatement les opérations en attente à la base.
        assertNotNull(em.find(Offre.class, offre.getId())); // Vérifie que supprimer la candidature n'a pas supprimé son offre.
    } // Ferme le bloc précédent (méthode, condition ou classe).

    @Test // Indique que JUnit doit exécuter cette méthode comme un test.
    void interditDeSupprimerUneOffreAvecUneCandidature() { // Teste la protection de la relation par la clé étrangère.
        Offre offre = creerOffre(); // Crée et enregistre une offre de test dans la transaction.
        creerCandidature(offre); // Ajoute une candidature pour que l'offre soit référencée.
        em.remove(offre); // Essaie de supprimer une offre qui possède encore une candidature.
        assertThrows(PersistenceException.class, () -> em.flush()); // Vérifie que la clé étrangère bloque cette suppression.
    } // Ferme le bloc précédent (méthode, condition ou classe).

    private Offre creerOffre() { // Regroupe la préparation d'une offre valide pour les tests MySQL.
        Offre offre = new Offre(); // Crée une offre Java vide pour le test.
        offre.setTitre("Offre de test transactionnel"); // Donne un titre à l'offre de test.
        offre.setContrat(ContratType.CDI); // Choisit le contrat CDI.
        offre.setDateExpiration(LocalDate.now().plusDays(30)); // Fixe la date d'expiration à trente jours après aujourd'hui.
        offre.setRecruteurId(1L); // Utilise un identifiant de recruteur de test, sans vérifier un autre service.
        offre.setCompetences(List.of("Java", "SQL")); // Ajoute deux compétences à l'offre.
        return em.persistAndFlush(offre); // Insère l'offre, envoie l'INSERT à MySQL et retourne l'objet avec son identifiant.
    } // Ferme le bloc précédent (méthode, condition ou classe).

    private Candidature creerCandidature(Offre offre) { // Prépare une candidature liée à l'offre reçue en paramètre.
        Candidature candidature = new Candidature(); // Crée une candidature Java vide pour le test.
        candidature.setOffre(offre); // Relie la candidature à son offre.
        candidature.setUserId(2L); // Utilise un identifiant de candidat de test, sans vérification externe.
        candidature.setCvId(3L); // Utilise un identifiant de CV de test, sans vérifier le service Profils.
        return em.persistAndFlush(candidature); // Insère la candidature ; JPA déclenche aussi la date et le statut initiaux.
    } // Ferme le bloc précédent (méthode, condition ou classe).
} // Ferme le bloc précédent (méthode, condition ou classe).
