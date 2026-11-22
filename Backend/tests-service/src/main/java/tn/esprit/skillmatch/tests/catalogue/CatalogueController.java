package tn.esprit.skillmatch.tests.catalogue;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tests")
public class CatalogueController {

    private final CatalogueTests catalogue;

    public CatalogueController(CatalogueTests catalogue) {
        this.catalogue = catalogue;
    }

    @GetMapping
    public List<VueTest> lister(@RequestParam(required = false) String competence) {
        return catalogue.lister(competence);
    }

    @GetMapping("/{id}")
    public VueTest lire(@PathVariable @Positive Long id) {
        return catalogue.lire(id);
    }

    @PostMapping
    public ResponseEntity<VueTest> creer(@Valid @RequestBody DefinitionTest definition) {
        VueTest test = catalogue.creer(definition);
        return ResponseEntity.created(URI.create("/api/tests/" + test.id())).body(test);
    }

    @PutMapping("/{id}")
    public VueTest modifier(@PathVariable @Positive Long id, @Valid @RequestBody DefinitionTest definition) {
        return catalogue.modifier(id, definition);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimer(@PathVariable @Positive Long id) {
        catalogue.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
