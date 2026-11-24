package tn.esprit.skillmatch.alertes.dto;

import jakarta.validation.constraints.NotNull;

public record ActiveRequest(
        @NotNull Boolean active
) {
}
