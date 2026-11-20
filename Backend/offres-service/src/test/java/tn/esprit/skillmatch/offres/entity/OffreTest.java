package tn.esprit.skillmatch.offres.entity;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OffreTest {
    @Test
    void expirationRespecteLaDateDuJour() {
        Offre offre = new Offre();
        offre.setDateExpiration(LocalDate.now().minusDays(1));
        assertTrue(offre.estExpiree());
        offre.setDateExpiration(LocalDate.now());
        assertFalse(offre.estExpiree());
        offre.setDateExpiration(LocalDate.now().plusDays(1));
        assertFalse(offre.estExpiree());
    }

    @Test
    void scoreIgnoreLaCasseLesEspacesEtLesDoublons() {
        Offre offre = new Offre();
        offre.setCompetences(List.of(" Java ", "JAVA", "SQL"));
        assertEquals(50.0, offre.calculerScore(List.of("java", "java")));
        assertEquals(100.0, offre.calculerScore(List.of("Java", "sql")));
        assertEquals(0.0, offre.calculerScore(null));
        offre.setCompetences(List.of());
        assertEquals(0.0, offre.calculerScore(List.of("Java")));
    }
}
