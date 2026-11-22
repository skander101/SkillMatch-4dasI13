package tn.esprit.skillmatch.entreprises.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.skillmatch.entreprises.dto.AvisRequest;
import tn.esprit.skillmatch.entreprises.dto.AvisResponse;
import tn.esprit.skillmatch.entreprises.service.AvisService;

import java.util.List;

@RestController
@RequestMapping("/skillmatch/entreprises")
@Tag(name = "Avis", description = "Publication et consultation des avis entreprises")
public class AvisController {

    private final AvisService avisService;

    public AvisController(AvisService avisService) {
        this.avisService = avisService;
    }

    @GetMapping("/{entrepriseId}/avis")
    @Operation(summary = "Lister les avis d'une entreprise")
    public List<AvisResponse> findByEntreprise(@PathVariable Long entrepriseId) {
        return avisService.findByEntreprise(entrepriseId);
    }

    @PostMapping("/{entrepriseId}/avis")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Publier un avis sur une entreprise")
    public AvisResponse submit(@PathVariable Long entrepriseId, @Valid @RequestBody AvisRequest request) {
        return avisService.submit(entrepriseId, request);
    }
}
