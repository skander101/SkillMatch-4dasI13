package tn.esprit.skillmatch.offres.entity;

import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import tn.esprit.skillmatch.offres.enums.ContratType;
import tn.esprit.skillmatch.offres.enums.CandidatureStatut;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Test manuel avec MySQL/XAMPP : mvn -Dtest=EntityPersistenceIT test.
// Les donnees sont annulees en fin de test ; les identifiants peuvent avancer.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class EntityPersistenceIT {
    @Autowired
    private TestEntityManager em;

    @Test
    void enregistreLesCompetencesEtLaRelationOffreCandidature() {
        Offre offre = creerOffre();
        Candidature candidature = creerCandidature(offre);
        em.clear();

        Offre relue = em.find(Offre.class, offre.getId());
        Candidature recue = em.find(Candidature.class, candidature.getId());
        assertEquals(List.of("Java", "SQL"), relue.getCompetences());
        assertEquals(offre.getId(), recue.getOffre().getId());
        assertEquals(CandidatureStatut.RECUE, recue.getStatut());
        assertNotNull(recue.getDateCandidature());

        em.remove(recue);
        em.flush();
        assertNotNull(em.find(Offre.class, offre.getId()));
    }

    @Test
    void interditDeSupprimerUneOffreAvecUneCandidature() {
        Offre offre = creerOffre();
        creerCandidature(offre);
        em.remove(offre);
        assertThrows(PersistenceException.class, () -> em.flush());
    }

    private Offre creerOffre() {
        Offre offre = new Offre();
        offre.setTitre("Offre de test transactionnel");
        offre.setContrat(ContratType.CDI);
        offre.setDateExpiration(LocalDate.now().plusDays(30));
        offre.setRecruteurId(1L);
        offre.setCompetences(List.of("Java", "SQL"));
        return em.persistAndFlush(offre);
    }

    private Candidature creerCandidature(Offre offre) {
        Candidature candidature = new Candidature();
        candidature.setOffre(offre);
        candidature.setUserId(2L);
        candidature.setCvId(3L);
        return em.persistAndFlush(candidature);
    }
}
