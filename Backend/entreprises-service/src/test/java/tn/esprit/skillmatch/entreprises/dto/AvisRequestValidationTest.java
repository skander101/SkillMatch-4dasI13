package tn.esprit.skillmatch.entreprises.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AvisRequestValidationTest {

    @Test
    void validRequestHasNoViolations() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();

            assertTrue(validator.validate(new AvisRequest(5, "Super")).isEmpty());
        }
    }

    @Test
    void noteOutsideOneToFiveIsRejected() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();

            assertFalse(validator.validate(new AvisRequest(0, "Trop bas")).isEmpty());
            assertFalse(validator.validate(new AvisRequest(6, "Trop haut")).isEmpty());
        }
    }

    @Test
    void noteAndCommentaireAreRequired() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();

            assertFalse(validator.validate(new AvisRequest(null, "Super")).isEmpty());
            assertFalse(validator.validate(new AvisRequest(5, " ")).isEmpty());
        }
    }
}
