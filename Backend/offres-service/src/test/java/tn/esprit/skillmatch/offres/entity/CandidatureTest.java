package tn.esprit.skillmatch.offres.entity;

import org.junit.jupiter.api.Test;
import tn.esprit.skillmatch.offres.enums.CandidatureStatut;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class CandidatureTest {
    @Test
    void creationInitialiseLeStatutEtLaDate() {
        Candidature candidature = new Candidature();
        LocalDateTime avant = LocalDateTime.now();
        candidature.initialiser();
        assertEquals(CandidatureStatut.RECUE, candidature.getStatut());
        assertFalse(candidature.getDateCandidature().isBefore(avant));
        assertFalse(candidature.getDateCandidature().isAfter(LocalDateTime.now()));
    }

    @Test
    void changerStatutRefuseUneValeurNulle() {
        Candidature candidature = new Candidature();
        candidature.changerStatut(CandidatureStatut.PRESELECTIONNEE);
        assertEquals(CandidatureStatut.PRESELECTIONNEE, candidature.getStatut());
        assertThrows(NullPointerException.class, () -> candidature.changerStatut(null));
    }
}
