package tn.esprit.skillmatch.offres.security; // Tests de la vraie vérification cryptographique.

import io.jsonwebtoken.Jwts; // Même construction que commun-service.
import io.jsonwebtoken.security.Keys; // Clé HMAC.
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class JwtVerifierTest {
    private final String secret = UUID.randomUUID().toString(); // Clé de test générée en mémoire.
    private final JwtVerifier verifier = new JwtVerifier(secret);

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "RECRUTEUR", "CANDIDAT"})
    void accepteLesTroisRolesAvecUnJwtValide(String role) {
        var claims = verifier.verifier(token(role, Instant.now().plusSeconds(120), "test@example.invalid", secret));
        assertEquals(role, claims.get("role", String.class));
        assertEquals("test@example.invalid", claims.getSubject()); // L'email est le subject, comme dans commun-service.
    }

    @Test
    void refuseSignatureFausseEtExpirationPassee() {
        refuse(token("ADMIN", Instant.now().plusSeconds(120), "test@example.invalid", UUID.randomUUID().toString()));
        refuse(token("ADMIN", Instant.now().minusSeconds(60), "test@example.invalid", secret));
    }

    @Test
    void exigeRoleEmailEtExpiration() {
        refuse(token("SUPERUSER", Instant.now().plusSeconds(120), "test@example.invalid", secret));
        refuse(token(null, Instant.now().plusSeconds(120), "test@example.invalid", secret));
        refuse(token("ADMIN", null, "test@example.invalid", secret)); // Pas de jeton sans limite.
        refuse(token("ADMIN", Instant.now().plusSeconds(120), null, secret));
    }

    @Test
    void refuseBearerAbsentJwtMalformeEtJetonNonSigne() {
        refuse(null);
        refuse("Bearer ");
        refuse("Basic exemple");
        refuse("Bearer faux");
        refuse("Bearer " + Jwts.builder().subject("test@example.invalid").compact()); // JWT sans signature.
    }

    private String token(String role, Instant expiration, String email, String cle) {
        return "Bearer " + Jwts.builder().subject(email).claim("role", role)
                .expiration(expiration == null ? null : Date.from(expiration))
                .signWith(Keys.hmacShaKeyFor(cle.getBytes(StandardCharsets.UTF_8))).compact();
    }

    private void refuse(String token) {
        assertEquals(401, assertThrows(ResponseStatusException.class, () -> verifier.verifier(token)).getStatusCode().value());
    }
}
