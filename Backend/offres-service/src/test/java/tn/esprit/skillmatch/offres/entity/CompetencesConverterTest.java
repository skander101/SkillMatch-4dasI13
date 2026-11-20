package tn.esprit.skillmatch.offres.entity; // Place ce fichier dans le package indiqué, qui organise les classes du service.

import org.junit.jupiter.api.Test; // Importe l'annotation qui désigne un test JUnit.
import java.util.List; // Importe le type liste.

import static org.junit.jupiter.api.Assertions.*; // Importe les vérifications JUnit comme assertEquals et assertTrue.

class CompetencesConverterTest { // Regroupe les tests de CompetencesConverterTest.
    private final CompetencesConverter converter = new CompetencesConverter(); // Crée le convertisseur à tester.

    @Test // Indique que JUnit doit exécuter cette méthode comme un test.
    void conserveLesCaracteresSpeciauxDansLeJson() { // Vérifie qu'un aller-retour JSON conserve exactement les textes.
        List<String> competences = List.of("C++", "Analyse de données", "Java \"avancé\""); // Prépare des compétences contenant des accents et des guillemets.
        assertEquals(competences, // Attend de retrouver exactement la liste de départ après conversion.
                converter.convertToEntityAttribute(converter.convertToDatabaseColumn(competences))); // Convertit la liste en JSON puis relit ce JSON en liste.
    } // Ferme le bloc précédent (méthode, condition ou classe).

    @Test // Indique que JUnit doit exécuter cette méthode comme un test.
    void gereLesValeursVidesEtRefuseLeJsonInvalide() { // Teste les valeurs absentes et un JSON incorrect.
        assertEquals("[]", converter.convertToDatabaseColumn(null)); // Vérifie qu'une liste absente devient le JSON d'une liste vide.
        assertTrue(converter.convertToEntityAttribute(null).isEmpty()); // Vérifie qu'un texte absent donne une liste vide.
        assertTrue(converter.convertToEntityAttribute("").isEmpty()); // Vérifie qu'un texte vide donne une liste vide.
        assertThrows(IllegalArgumentException.class, // Attend une erreur de conversion pour un texte invalide.
                () -> converter.convertToEntityAttribute("pas du JSON")); // Essaie de lire volontairement un texte qui n'est pas du JSON.
    } // Ferme le bloc précédent (méthode, condition ou classe).
} // Ferme le bloc précédent (méthode, condition ou classe).
