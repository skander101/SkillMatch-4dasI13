package tn.esprit.skillmatch.tests.tentatives;

import java.time.Instant;

public record ResultatTestVue(
        Long id,
        Long testId,
        Long userId,
        String competence,
        int score,
        boolean reussi,
        int tentative,
        Instant date
) {
    static ResultatTestVue de(ResultatTest resultat) {
        return new ResultatTestVue(
                resultat.getId(),
                resultat.getTestId(),
                resultat.getCandidatId(),
                resultat.getCompetence(),
                resultat.getScore(),
                resultat.isReussi(),
                resultat.getTentative(),
                resultat.getDate()
        );
    }
}
