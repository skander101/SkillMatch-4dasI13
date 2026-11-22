package tn.esprit.skillmatch.tests.tentatives;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.skillmatch.tests.catalogue.Question;
import tn.esprit.skillmatch.tests.catalogue.TestTechnique;
import tn.esprit.skillmatch.tests.catalogue.TestTechniqueRepository;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest(showSql = false)
@Import({TentativeService.class, ConsultationResultatsService.class})
class ConsultationResultatsServiceTest {
    @Autowired
    private TestTechniqueRepository tests;

    @Autowired
    private TentativeService tentatives;

    @Autowired
    private ConsultationResultatsService consultation;

    @Test
    void separeLesHistoriquesEtNeRetourneQueLaReussiteDuTestDemande() {
        List<Question> questions = List.of(
                new Question("q1", "Type primitif ?", List.of("int", "String"), 0),
                new Question("q2", "Commande de lecture Git ?", List.of("log", "merge"), 0)
        );
        Long premier = tests.save(new TestTechnique("Java bases", "Java", 15, 50, questions)).getId();
        Long second = tests.save(new TestTechnique("Java pratique", "JAVA", 15, 50, questions)).getId();
        long candidatId = 41L;

        tentatives.soumettre(second, candidatId, Map.of("q1", 1));
        var reussite = tentatives.soumettre(second, candidatId, Map.of("q1", 0, "q2", 0));
        tentatives.soumettre(second, 42L, Map.of("q1", 0));

        assertThat(consultation.historique(candidatId)).hasSize(2)
                .allSatisfy(resultat -> assertThat(resultat.userId()).isEqualTo(candidatId));
        assertThat(consultation.resultatsDuTest(second)).hasSize(3);
        assertThat(consultation.resultatsDuTest(premier)).isEmpty();
        assertThat(consultation.verifierReussite(candidatId, second)).isEqualTo(reussite);
        assertThat(assertThrows(ResponseStatusException.class,
                () -> consultation.verifierReussite(candidatId, premier)).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
