package tn.esprit.skillmatch.tests.tentatives;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.skillmatch.tests.security.Identite;

import java.util.Map;

@RestController
@RequestMapping("/api/tests/{testId}")
public class TentativeController {
    private final TentativeService service;

    public TentativeController(TentativeService service) {
        this.service = service;
    }

    @PostMapping("/submit")
    public ResultatTestVue soumettre(
            @PathVariable @Positive Long testId,
            @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal,
            @Valid @RequestBody EnvoiReponses envoi
    ) {
        return service.soumettre(testId, Identite.candidatId(principal), envoi.reponses());
    }

    public record EnvoiReponses(@NotEmpty @Size(max = 100)
                                Map<@NotBlank String, @NotNull @PositiveOrZero Integer> reponses) {
    }
}
