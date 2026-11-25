package tn.esprit.skillmatch.profils.event;

public record TestPassedData(
        Long userId,
        String testTitle,
        String badge
) {
}
