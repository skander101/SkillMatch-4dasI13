package tn.esprit.skillmatch.messagerie.exception;

public class AccessForbiddenException extends RuntimeException {
    public AccessForbiddenException(String message) {
        super(message);
    }
}
