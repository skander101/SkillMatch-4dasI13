package tn.esprit.skillmatch.tests.tentatives;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.skillmatch.tests.catalogue.Question;
import tn.esprit.skillmatch.tests.catalogue.TestTechnique;
import tn.esprit.skillmatch.tests.catalogue.TestTechniqueRepository;
import tn.esprit.skillmatch.tests.events.TestPassed;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TentativeService {
    private static final int MAX_TENTATIVES = 3;

    private final TestTechniqueRepository tests;
    private final ResultatTestRepository resultats;
    private final ApplicationEventPublisher evenements;

    public TentativeService(TestTechniqueRepository tests, ResultatTestRepository resultats,
                            ApplicationEventPublisher evenements) {
        this.tests = tests;
        this.resultats = resultats;
        this.evenements = evenements;
    }

    @Transactional
    public ResultatTestVue soumettre(Long testId, Long candidatId, Map<String, Integer> reponses) {
        // Toutes les soumissions du même test prennent ce verrou avant de compter les tentatives.
        TestTechnique test = tests.findLockedById(testId)
                .orElseThrow(TentativeService::testIntrouvable);
        if (resultats.findFirstByTest_IdAndCandidatIdAndReussiTrueOrderByTentativeAsc(testId, candidatId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce test a déjà été réussi.");
        }
        int utilisees = resultats.countByTest_IdAndCandidatId(testId, candidatId);
        if (utilisees >= MAX_TENTATIVES) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Les trois tentatives ont été utilisées.");
        }

        int score = corriger(test.getQuestions(), reponses);
        ResultatTest resultat = resultats.save(new ResultatTest(test, candidatId, score, utilisees + 1, Instant.now()));
        if (resultat.isReussi()) {
            evenements.publishEvent(new TestPassed(resultat.getId(), test.getId(), candidatId,
                    resultat.getCompetence(), score, LocalDateTime.ofInstant(resultat.getDate(), ZoneOffset.UTC)));
        }
        return ResultatTestVue.de(resultat);
    }

    static int corriger(List<Question> questions, Map<String, Integer> reponses) {
        if (reponses.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Au moins une réponse est requise.");
        }
        Map<String, Question> questionsParId = questions.stream()
                .collect(Collectors.toMap(Question::id, Function.identity()));

        int bonnes = 0;
        for (Map.Entry<String, Integer> reponse : reponses.entrySet()) {
            Question question = questionsParId.get(reponse.getKey());
            if (question == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Une réponse cible une question inconnue.");
            }
            Integer index = reponse.getValue();
            if (index == null || index < 0 || index >= question.options().size()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Une réponse cible une option inconnue.");
            }
            if (index == question.indexBonneReponse()) {
                bonnes++;
            }
        }
        return (int) (100L * bonnes / questions.size());
    }

    private static ResponseStatusException testIntrouvable() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Test introuvable.");
    }

}
