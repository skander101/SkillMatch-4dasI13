package tn.esprit.skillmatch.commun.exception;

public record ApiError(
        int status,
        String message
) {
}
