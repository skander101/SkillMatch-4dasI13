package tn.esprit.skillmatch.tests.tentatives;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.skillmatch.tests.catalogue.Question;
import tn.esprit.skillmatch.tests.catalogue.TestTechnique;
import tn.esprit.skillmatch.tests.catalogue.TestTechniqueRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(showSql = false)
@Import(TentativeService.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TentativesConcurrentesTest {
    @Autowired
    private TestTechniqueRepository tests;

    @Autowired
    private ResultatTestRepository resultats;

    @Autowired
    private TentativeService service;

    @Test
    void quatreEnvoisParallelesNeCreentQueTroisTentatives() throws Exception {
        var question = new Question("q1", "Quel outil compile le Java ?", List.of("javac", "git"), 0);
        Long testId = tests.save(new TestTechnique("Java", "Java", 15, 50, List.of(question))).getId();
        long candidatId = 47L;
        var depart = new CountDownLatch(1);
        var executeur = Executors.newFixedThreadPool(4);
        try {
            List<Future<Integer>> envois = new ArrayList<>();
            for (int index = 0; index < 4; index++) {
                envois.add(executeur.submit(() -> {
                    depart.await();
                    try {
                        return service.soumettre(testId, candidatId, Map.of("q1", 1)).tentative();
                    } catch (ResponseStatusException exception) {
                        return exception.getStatusCode().value();
                    }
                }));
            }
            depart.countDown();

            List<Integer> reponses = new ArrayList<>();
            for (Future<Integer> envoi : envois) {
                reponses.add(envoi.get(15, TimeUnit.SECONDS));
            }
            assertThat(reponses).containsExactlyInAnyOrder(1, 2, 3, HttpStatus.CONFLICT.value());
            assertThat(resultats.countByTest_IdAndCandidatId(testId, candidatId)).isEqualTo(3);
        } finally {
            executeur.shutdownNow();
        }
    }
}
