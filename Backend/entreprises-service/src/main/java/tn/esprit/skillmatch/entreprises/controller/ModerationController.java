package tn.esprit.skillmatch.entreprises.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.skillmatch.entreprises.dto.AvisResponse;
import tn.esprit.skillmatch.entreprises.dto.ModerationRequest;
import tn.esprit.skillmatch.entreprises.service.AvisService;

@RestController
@RequestMapping("/skillmatch/avis")
@Tag(name = "Modération", description = "Modération des avis et recalcul de la note moyenne")
public class ModerationController {

    private final AvisService avisService;

    public ModerationController(AvisService avisService) {
        this.avisService = avisService;
    }

    @PutMapping("/{id}/moderation")
    @Operation(summary = "Approuver ou rejeter un avis")
    public AvisResponse moderate(@PathVariable Long id, @Valid @RequestBody ModerationRequest request) {
        return avisService.moderate(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Supprimer un avis")
    public void delete(@PathVariable Long id) {
        avisService.delete(id);
    }
}
