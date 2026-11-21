package tn.esprit.skillmatch.entreprises.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.skillmatch.entreprises.dto.EntrepriseRequest;
import tn.esprit.skillmatch.entreprises.dto.EntrepriseResponse;
import tn.esprit.skillmatch.entreprises.service.EntrepriseService;

import java.util.List;

@RestController
@RequestMapping("/skillmatch/entreprises")
@Tag(name = "Entreprises", description = "CRUD des entreprises")
public class EntrepriseController {

    private final EntrepriseService entrepriseService;

    public EntrepriseController(EntrepriseService entrepriseService) {
        this.entrepriseService = entrepriseService;
    }

    @GetMapping
    @Operation(summary = "Lister toutes les entreprises")
    public List<EntrepriseResponse> findAll() {
        return entrepriseService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Récupérer une entreprise par son identifiant")
    public EntrepriseResponse findById(@PathVariable Long id) {
        return entrepriseService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Créer une entreprise")
    public EntrepriseResponse create(@Valid @RequestBody EntrepriseRequest request) {
        return entrepriseService.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier une entreprise")
    public EntrepriseResponse update(@PathVariable Long id, @Valid @RequestBody EntrepriseRequest request) {
        return entrepriseService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Supprimer une entreprise")
    public void delete(@PathVariable Long id) {
        entrepriseService.delete(id);
    }
}
