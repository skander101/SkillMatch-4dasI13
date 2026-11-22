package tn.esprit.skillmatch.tests.tentatives;

import jakarta.validation.constraints.Positive;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.skillmatch.tests.security.Identite;

import java.util.List;

@RestController
public class ConsultationResultatsController {
    private final ConsultationResultatsService service;

    public ConsultationResultatsController(ConsultationResultatsService service) {
        this.service = service;
    }

    @GetMapping("/api/results/me")
    public List<ResultatTestVue> historique(@AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal) {
        return service.historique(Identite.candidatId(principal));
    }

    @GetMapping("/api/tests/{testId}/results")
    public List<ResultatTestVue> resultatsDuTest(@PathVariable @Positive Long testId) {
        return service.resultatsDuTest(testId);
    }

    @GetMapping("/api/results/check")
    public ResultatTestVue verifierReussite(
            @RequestParam @Positive Long userId,
            @RequestParam @Positive Long testId
    ) {
        return service.verifierReussite(userId, testId);
    }
}
