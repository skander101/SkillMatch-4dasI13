package tn.esprit.skillmatch.offres.security; // Vérification locale du JWT.

import io.jsonwebtoken.*; // Lecture des claims et erreurs JWT.
import io.jsonwebtoken.security.Keys; // Même clé HMAC que commun-service.
import org.springframework.beans.factory.annotation.Value; // Variable de configuration.
import org.springframework.http.HttpStatus; // Réponse 401.
import org.springframework.stereotype.Component; // Objet fourni par Spring.
import org.springframework.web.server.ResponseStatusException; // Erreur HTTP.
import javax.crypto.SecretKey; // Clé de signature.
import java.nio.charset.StandardCharsets; // Encodage identique au service commun.
import java.util.Set; // Trois rôles autorisés.

@Component
public class JwtVerifier {
    private final SecretKey key; // Ne stocke jamais la clé dans le code.

    public JwtVerifier(@Value("${jwt.secret}") String secret) {
        key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)); // Au moins 32 octets.
    }

    public Claims verifier(String authorization) { // Vérifie avant tout appel au service commun.
        if (authorization == null || !authorization.startsWith("Bearer ") || authorization.substring(7).isBlank())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentification requise");
        try {
            Claims claims = Jwts.parser().verifyWith(key).build() // Vérifie la signature et l'expiration.
                    .parseSignedClaims(authorization.substring(7)).getPayload(); // Refuse les JWT non signés.
            String role = claims.get("role", String.class); // Ne vient jamais du formulaire Angular.
            if (claims.getSubject() == null || claims.getSubject().isBlank() || claims.getExpiration() == null
                    || role == null || !Set.of("ADMIN", "RECRUTEUR", "CANDIDAT").contains(role))
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Claims JWT incomplets ou rôle invalide");
            return claims; // Contient l'email et le rôle vérifiés, pas un userId supposé.
        } catch (JwtException | IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "JWT invalide ou expiré"); // Aucun détail de clé exposé.
        }
    }
}
