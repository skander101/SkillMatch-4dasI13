package tn.esprit.skillmatch.offres.entity; // Place ce fichier dans le package indiqué, qui organise les classes du service.

import org.junit.jupiter.api.Test; // Importe l'annotation qui désigne un test JUnit.
import tn.esprit.skillmatch.offres.enums.CandidatureStatut; // Importe les valeurs autorisées de CandidatureStatut.

import java.time.LocalDateTime; // Importe les dates avec l'heure.

import static org.junit.jupiter.api.Assertions.*; // Importe les vérifications JUnit comme assertEquals et assertTrue.

class CandidatureTest { // Regroupe les tests de CandidatureTest.
    @Test // Indique que JUnit doit exécuter cette méthode comme un test.
    void creationInitialiseLeStatutEtLaDate() { // Vérifie les valeurs automatiques d'une nouvelle candidature.
        Candidature candidature = new Candidature(); // Crée une candidature Java vide pour le test.
        LocalDateTime avant = LocalDateTime.now(); // Mémorise l'instant juste avant l'initialisation.
        candidature.initialiser(); // Appelle la méthode normalement exécutée par JPA à l'insertion.
        assertEquals(CandidatureStatut.RECUE, candidature.getStatut()); // Vérifie que le statut initial est RECUE.
        assertFalse(candidature.getDateCandidature().isBefore(avant)); // Vérifie que la date n'est pas antérieure au début de l'initialisation.
        assertFalse(candidature.getDateCandidature().isAfter(LocalDateTime.now())); // Vérifie que la date n'est pas dans le futur.
    } // Ferme le bloc précédent (méthode, condition ou classe).

    @Test // Indique que JUnit doit exécuter cette méthode comme un test.
    void changerStatutRefuseUneValeurNulle() { // Teste un changement de statut valide puis le refus de null.
        Candidature candidature = new Candidature(); // Crée une candidature Java vide pour le test.
        candidature.changerStatut(CandidatureStatut.PRESELECTIONNEE); // Passe la candidature au statut PRESELECTIONNEE.
        assertEquals(CandidatureStatut.PRESELECTIONNEE, candidature.getStatut()); // Vérifie que le nouveau statut a été conservé.
        assertThrows(NullPointerException.class, () -> candidature.changerStatut(null)); // Vérifie qu'un changement vers null provoque bien une erreur.
    } // Ferme le bloc précédent (méthode, condition ou classe).
} // Ferme le bloc précédent (méthode, condition ou classe).
