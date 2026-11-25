
package tn.esprit.skillmatch.offres.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Set;


// ========== VÉRIFICATION DU JWT ==========

// Déclare cette classe comme un composant géré par Spring.
@Component

// Classe qui vérifie la validité des tokens JWT.
public class JwtVerifier {


    // ========== CLÉ DE SIGNATURE ==========

    // Stocke la clé secrète utilisée pour vérifier les JWT.
    private final SecretKey key;


    // ========== CONSTRUCTEUR ==========

    // Récupère la clé secrète depuis la configuration du projet.
    public JwtVerifier(@Value("${jwt.secret}") String secret) {

        // Transforme la clé secrète en clé de signature HMAC.
        key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }


    // ========== 1. VÉRIFIER LE TOKEN JWT ==========

    // Méthode qui vérifie le token envoyé par l'utilisateur.
    public Claims verifier(String authorization) {

        // Vérifie que le token existe et commence par "Bearer ".
        if (authorization == null || !authorization.startsWith("Bearer ") || authorization.substring(7).isBlank())

            // Retourne une erreur 401 si le token est absent ou mal présenté.
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentification requise");


        // ========== VÉRIFICATION DE LA SIGNATURE ==========

        // Essaie de vérifier le JWT.
        try {

            // Vérifie la signature du token avec la clé secrète.
            Claims claims = Jwts.parser().verifyWith(key).build()

                // Lit les informations du token et vérifie son expiration.
                .parseSignedClaims(authorization.substring(7)).getPayload();


            // ========== RÉCUPÉRATION DU RÔLE ==========

            // Récupère le rôle de l'utilisateur depuis le JWT.
            String role = claims.get("role", String.class);


            // ========== VALIDATION DES INFORMATIONS ==========

            // Vérifie que l'identité, l'expiration et le rôle sont valides.
            if (claims.getSubject() == null || claims.getSubject().isBlank() || claims.getExpiration() == null

                // Accepte uniquement ADMIN, RECRUTEUR ou CANDIDAT.
                || role == null || !Set.of("ADMIN", "RECRUTEUR", "CANDIDAT").contains(role))

                // Retourne une erreur 401 si les informations sont invalides.
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Claims JWT incomplets ou rôle invalide");


            // ========== RETOUR DU JWT VÉRIFIÉ ==========

            // Retourne les informations vérifiées du token.
            return claims;


            // ========== GESTION DES ERREURS ==========

            // Intercepte les erreurs de JWT ou de données invalides.
        } catch (JwtException | IllegalArgumentException exception) {

            // Retourne une erreur 401 si le JWT est invalide ou expiré.
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "JWT invalide ou expiré");
        }
    }
}
