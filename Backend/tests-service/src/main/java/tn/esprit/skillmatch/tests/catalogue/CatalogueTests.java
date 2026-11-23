package tn.esprit.skillmatch.tests.catalogue;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.skillmatch.tests.tentatives.ResultatTestRepository;

@Service
public class CatalogueTests {

    private final TestTechniqueRepository repository;
    private final ResultatTestRepository resultats;

    public CatalogueTests(TestTechniqueRepository repository, ResultatTestRepository resultats) {
        this.repository = repository;
        this.resultats = resultats;
    }

    @Transactional(readOnly = true)
    public List<VueTest> lister(String competence) {
        List<TestTechnique> tests = competence == null || competence.isBlank()
                ? repository.findAll(Sort.by("id"))
                : repository.findAllByCompetenceIgnoreCaseOrderByIdAsc(competence.strip());
        return tests.stream().map(VueTest::de).toList();
    }

    @Transactional(readOnly = true)
    public VueTest lire(Long id) {
        TestTechnique test = repository.findById(id).orElseThrow(CatalogueTests::testIntrouvable);
        return VueTest.de(test);
    }

    @Transactional(readOnly = true)
    public DefinitionTest lireDefinition(Long id) {
        TestTechnique test = repository.findById(id).orElseThrow(CatalogueTests::testIntrouvable);
        return DefinitionTest.de(test);
    }

    @Transactional
    public VueTest creer(DefinitionTest definition) {
        try {
            var test = new TestTechnique(definition.titre(), definition.competence(), definition.dureeMinutes(),
                    definition.seuilReussite(), definition.construireQuestions());
            return VueTest.de(repository.save(test));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }

    @Transactional
    public VueTest modifier(Long id, DefinitionTest definition) {
        TestTechnique test = verrouillerTestSansResultat(id);
        try {
            test.modifier(definition.titre(), definition.competence(), definition.dureeMinutes(),
                    definition.seuilReussite(), definition.construireQuestions());
            return VueTest.de(test);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }

    @Transactional
    public void supprimer(Long id) {
        repository.delete(verrouillerTestSansResultat(id));
    }

    private TestTechnique verrouillerTestSansResultat(Long id) {
        TestTechnique test = repository.findLockedById(id).orElseThrow(CatalogueTests::testIntrouvable);
        if (resultats.existsByTest_Id(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Les résultats existants verrouillent ce test.");
        }
        return test;
    }

    private static ResponseStatusException testIntrouvable() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Test introuvable.");
    }
}
