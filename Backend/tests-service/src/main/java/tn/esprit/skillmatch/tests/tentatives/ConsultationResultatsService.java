package tn.esprit.skillmatch.tests.tentatives;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.skillmatch.tests.catalogue.TestTechniqueRepository;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ConsultationResultatsService {
    private final TestTechniqueRepository tests;
    private final ResultatTestRepository resultats;

    public ConsultationResultatsService(TestTechniqueRepository tests, ResultatTestRepository resultats) {
        this.tests = tests;
        this.resultats = resultats;
    }

    public List<ResultatTestVue> historique(Long candidatId) {
        return resultats.findByCandidatIdOrderByDateDescIdDesc(candidatId)
                .stream()
                .map(ResultatTestVue::de)
                .toList();
    }

    public List<ResultatTestVue> resultatsDuTest(Long testId) {
        if (!tests.existsById(testId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Test introuvable.");
        }
        return resultats.findByTest_IdOrderByDateDescIdDesc(testId)
                .stream()
                .map(ResultatTestVue::de)
                .toList();
    }

    public ResultatTestVue verifierReussite(Long candidatId, Long testId) {
        return resultats.findFirstByTest_IdAndCandidatIdAndReussiTrueOrderByTentativeAsc(testId, candidatId)
                .map(ResultatTestVue::de)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aucun résultat réussi."));
    }
}
