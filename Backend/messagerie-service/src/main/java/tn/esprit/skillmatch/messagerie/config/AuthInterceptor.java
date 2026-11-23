package tn.esprit.skillmatch.messagerie.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import tn.esprit.skillmatch.messagerie.exception.ApiError;
import tn.esprit.skillmatch.messagerie.exception.ServiceUnavailableException;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Intercepteur d'authentification JWT — calqué sur AdminInterceptor de commun-service.
 * Vérifie le token JWT, résout l'utilisateur via commun-service /auth/me (avec cache 5 min),
 * et place AuthenticatedUser dans request.setAttribute("authenticatedUser", user).
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AuthInterceptor.class);
    public static final String AUTHENTICATED_USER_ATTR = "authenticatedUser";

    private final SecretKey key;
    private final CommunClient communClient;
    private final ObjectMapper objectMapper;

    public AuthInterceptor(
            @Value("${jwt.secret}") String secret,
            CommunClient communClient,
            ObjectMapper objectMapper
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.communClient = communClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ") || authorization.length() <= 7) {
            writeError(response, 401, "Authentification requise");
            return false;
        }

        String token = authorization.substring(7);

        String email;
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            email = claims.getSubject();
            if (email == null || email.isBlank()) {
                writeError(response, 401, "Token JWT invalide ou expiré");
                return false;
            }
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Token JWT invalide : {}", e.getMessage());
            writeError(response, 401, "Token JWT invalide ou expiré");
            return false;
        }

        // Résolution de l'id numérique via commun-service /auth/me (avec cache 5 min)
        try {
            AuthenticatedUser user = communClient.resolveUser(email, token);
            request.setAttribute(AUTHENTICATED_USER_ATTR, user);
            return true;
        } catch (ServiceUnavailableException e) {
            log.error("commun-service indisponible : {}", e.getMessage());
            writeError(response, 503, e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Erreur lors de la résolution de l'utilisateur : {}", e.getMessage(), e);
            writeError(response, 503, "commun-service est injoignable : " + e.getMessage());
            return false;
        }
    }

    private void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(new ApiError(status, message)));
    }
}
