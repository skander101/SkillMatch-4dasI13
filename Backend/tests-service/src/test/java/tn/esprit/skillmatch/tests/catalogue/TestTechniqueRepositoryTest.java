package tn.esprit.skillmatch.tests.catalogue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(showSql = false)
class TestTechniqueRepositoryTest {

    @Autowired
    private TestTechniqueRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void conserveLeQcmDansUnAllerRetourEnBase() {
        var question = new Question("q1", "Quel type représente une date ?", List.of("LocalDate", "String"), 0);
        var test = repository.saveAndFlush(new TestTechnique("Date et heure", "Java", 20, 70, List.of(question)));
        entityManager.clear();

        var recharge = repository.findLockedById(test.getId()).orElseThrow();

        assertThat(recharge.getQuestions()).containsExactly(question);
        assertThat(repository.findAllByCompetenceIgnoreCaseOrderByIdAsc("java")).containsExactly(recharge);
    }
}
