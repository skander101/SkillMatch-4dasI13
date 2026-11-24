package tn.esprit.skillmatch.alertes.exception;

public record ApiError(
        int status,
        String message
) {
}
