package tn.esprit.skillmatch.tests.events;

import java.time.LocalDateTime;

public record TestPassed(
        Long resultatId,
        Long testId,
        Long userId,
        String competence,
        int score,
        LocalDateTime date
) {
}
