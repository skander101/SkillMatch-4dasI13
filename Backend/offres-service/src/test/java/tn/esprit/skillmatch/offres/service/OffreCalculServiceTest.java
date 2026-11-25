package tn.esprit.skillmatch.offres.service;

import tn.esprit.skillmatch.offres.entity.Offre; // Place ce fichier dans le package indiqué, qui organise les classes du service.

import org.junit.jupiter.api.Test; // Importe l'annotation qui désigne un test JUnit.
import java.time.LocalDate; // Importe les dates sans heure.
import java.util.List; // Importe le type liste.

import static org.junit.jupiter.api.Assertions.*; // Importe les vérifications JUnit comme assertEquals et assertTrue.

class OffreCalculServiceTest {
    private final OffreService service = new OffreService(null, null, null, null); // Regroupe les tests de OffreTest.
    @Test // Indique que JUnit doit exécuter cette méthode comme un test.
    void expirationRespecteLaDateDuJour() { // Teste l'expiration pour hier, aujourd'hui et demain.
        Offre offre = new Offre(); // Crée une offre Java vide pour le test.
        offre.setDateExpiration(LocalDate.now().minusDays(1)); // Choisit hier pour représenter une offre expirée.
        assertTrue(service.estExpiree(offre)); // Vérifie qu'une date passée rend l'offre expirée.
        offre.setDateExpiration(LocalDate.now()); // Choisit aujourd'hui comme dernier jour de validité.
        assertFalse(service.estExpiree(offre)); // Vérifie que l'offre n'est pas encore expirée.
        offre.setDateExpiration(LocalDate.now().plusDays(1)); // Choisit demain comme date d'expiration.
        assertFalse(service.estExpiree(offre)); // Vérifie que l'offre n'est pas encore expirée.
    } // Ferme le bloc précédent (méthode, condition ou classe).

    @Test // Indique que JUnit doit exécuter cette méthode comme un test.
    void scoreIgnoreLaCasseLesEspacesEtLesDoublons() { // Teste le calcul du score et les cas sans compétences.
        Offre offre = new Offre(); // Crée une offre Java vide pour le test.
        offre.setCompetences(List.of(" Java ", "JAVA", "SQL")); // Prépare des compétences avec espaces, casse différente et doublon.
        assertEquals(50.0, service.calculerScore(offre, List.of("java", "java"))); // Vérifie que Java couvre une compétence sur deux malgré les doublons.
        assertEquals(100.0, service.calculerScore(offre, List.of("Java", "sql"))); // Vérifie que toutes les compétences requises donnent 100 pour cent.
        assertEquals(0.0, service.calculerScore(offre, null)); // Vérifie qu'un profil absent donne un score nul.
        offre.setCompetences(List.of()); // Retire toutes les compétences requises.
        assertEquals(0.0, service.calculerScore(offre, List.of("Java"))); // Vérifie qu'une offre sans compétences donne zéro sans division par zéro.
    } // Ferme le bloc précédent (méthode, condition ou classe).
} // Ferme le bloc précédent (méthode, condition ou classe).
