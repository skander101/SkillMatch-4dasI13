package tn.esprit.skillmatch.alertes.entity;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlerteEmploiTest {

    private static final String TITRE = "Développeur Java Spring";
    private static final List<String> COMPETENCES = List.of("Java", "Spring Boot", "SQL");

    private AlerteEmploi alerte(List<String> motsCles, String localisation, ContratType contrat) {
        return new AlerteEmploi(1L, motsCles, localisation, contrat, Frequence.IMMEDIATE);
    }

    @Test
    void correspondParMotCleDansLeTitre() {
        assertTrue(alerte(List.of("java"), "Tunis", ContratType.CDI)
                .correspondA(TITRE, COMPETENCES, "Tunis", ContratType.CDI));
    }

    @Test
    void correspondParCompetence() {
        assertTrue(alerte(List.of("spring boot"), null, null)
                .correspondA("Ingénieur backend", COMPETENCES, "Sfax", ContratType.STAGE));
    }

    @Test
    void neCorrespondPasSiLeContratEstDifferent() {
        assertFalse(alerte(List.of("java"), null, ContratType.STAGE)
                .correspondA(TITRE, COMPETENCES, "Tunis", ContratType.CDI));
    }

    @Test
    void neCorrespondPasSiLaLocalisationEstDifferente() {
        assertFalse(alerte(List.of("java"), "Sfax", null)
                .correspondA(TITRE, COMPETENCES, "Tunis", ContratType.CDI));
    }

    @Test
    void neCorrespondPasSansMotCleCommun() {
        assertFalse(alerte(List.of("python"), null, null)
                .correspondA(TITRE, COMPETENCES, "Tunis", ContratType.CDI));
    }

    @Test
    void uneAlerteInactiveNeCorrespondJamais() {
        AlerteEmploi alerte = alerte(List.of("java"), null, null);
        alerte.setActive(false);
        assertFalse(alerte.correspondA(TITRE, COMPETENCES, "Tunis", ContratType.CDI));
    }
}
