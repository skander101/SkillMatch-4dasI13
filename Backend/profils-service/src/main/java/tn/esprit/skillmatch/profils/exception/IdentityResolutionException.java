package tn.esprit.skillmatch.profils.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class IdentityResolutionException extends ResponseStatusException {

    public IdentityResolutionException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }
}
