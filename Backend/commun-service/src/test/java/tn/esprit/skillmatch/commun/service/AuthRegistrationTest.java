package tn.esprit.skillmatch.commun.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.skillmatch.commun.dto.RegisterRequest;
import tn.esprit.skillmatch.commun.entity.*;
import tn.esprit.skillmatch.commun.repository.UtilisateurRepository;
import tn.esprit.skillmatch.commun.security.JwtService;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthRegistrationTest {
    private final UtilisateurRepository users = mock(UtilisateurRepository.class);
    private final PasswordEncoder passwords = mock(PasswordEncoder.class);
    private final JwtService jwt = mock(JwtService.class);
    private final AuthService service = new AuthService(users, passwords, jwt);

    @Test
    void refuseAdminAvantToutEnregistrement() {
        var erreur = assertThrows(ResponseStatusException.class, () -> service.register(demande(Role.ADMIN)));
        assertEquals(403, erreur.getStatusCode().value());
        verifyNoInteractions(users, passwords, jwt); // Aucun compte ni token ADMIN créé.
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"CANDIDAT", "RECRUTEUR"})
    void conserveLesDeuxRolesPublics(Role role) {
        when(passwords.encode("mot-de-passe-test")).thenReturn("hash-test");
        when(users.save(any())).thenAnswer(appel -> appel.getArgument(0));
        when(jwt.generateToken(any())).thenReturn("token-test");
        var reponse = service.register(demande(role));
        assertEquals(role, reponse.user().role());
        assertEquals("token-test", reponse.token());
        verify(users).save(any(Utilisateur.class));
    }

    private RegisterRequest demande(Role role) {
        return new RegisterRequest("Test", "test@example.invalid", "mot-de-passe-test", role);
    }
}
