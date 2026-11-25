package tn.esprit.skillmatch.commun.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.skillmatch.commun.dto.UserResponse;
import tn.esprit.skillmatch.commun.entity.Role;
import tn.esprit.skillmatch.commun.entity.Utilisateur;
import tn.esprit.skillmatch.commun.security.JwtService;
import tn.esprit.skillmatch.commun.service.AuthService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthControllerTest {

    private static final String SECRET = "skillmatch-local-development-secret-key-change-me";

    private final JwtService jwtService = new JwtService(SECRET, 86400000L);
    private RecordingAuthService authService;
    private AuthController controller;

    @BeforeEach
    void setUp() {
        UserResponse user = new UserResponse(10L, "Alice Martin", "alice@example.com", Role.CANDIDAT);
        authService = new RecordingAuthService(user);
        controller = new AuthController(authService, jwtService);
    }

    @Test
    void meReturnsCurrentUser() {
        String token = jwtService.generateToken(
                new Utilisateur("Alice Martin", "alice@example.com", "password", Role.CANDIDAT)
        );

        UserResponse response = controller.me("Bearer " + token);

        assertEquals(10L, response.id());
        assertEquals("alice@example.com", authService.email);
    }

    @Test
    void meRejectsMissingAuthorization() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                controller.me(null)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    @Test
    void meRejectsMalformedAuthorization() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                controller.me("token")
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    @Test
    void meRejectsInvalidToken() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                controller.me("Bearer invalid-token")
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    private static class RecordingAuthService extends AuthService {

        private final UserResponse user;
        private String email;

        private RecordingAuthService(UserResponse user) {
            super(null, null, null);
            this.user = user;
        }

        @Override
        public UserResponse currentUser(String email) {
            this.email = email;
            return user;
        }
    }
}
