package tn.esprit.skillmatch.profils.dto;

public record CompletenessResponse(
        Long userId,
        Integer completeness,
        boolean complete
) {
}
