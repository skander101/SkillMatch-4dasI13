package tn.esprit.skillmatch.tests.catalogue;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CatalogueTests {

    private final TestTechniqueRepository repository;

    public CatalogueTests(TestTechniqueRepository repository) {
        this.repository = repository;
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
        TestTechnique test = repository.findLockedById(id).orElseThrow(CatalogueTests::testIntrouvable);
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
        TestTechnique test = repository.findLockedById(id).orElseThrow(CatalogueTests::testIntrouvable);
        try {
            repository.delete(test);
            repository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ce test possède des résultats et ne peut pas être supprimé.", exception);
        }
    }

    private static ResponseStatusException testIntrouvable() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Test introuvable.");
    }
}
