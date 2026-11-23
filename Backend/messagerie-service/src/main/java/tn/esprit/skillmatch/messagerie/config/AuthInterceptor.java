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
 * Vérifie la signature et l'expiration du JWT, puis résout l'utilisateur via /auth/me si nécessaire (cache 10 min),
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
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ") || authorization.length() <= 7) {
            writeError(request, response, 401, "Authentification requise");
            return false;
        }

        String token = authorization.substring(7);

        String email;
        Claims claims;
        try {
            claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // Un JWT sans exp n'est pas accepté : l'expiration doit être explicitement vérifiable.
            if (claims.getExpiration() == null || !claims.getExpiration().after(new java.util.Date())) {
                writeError(request, response, 401, "Token JWT invalide ou expiré");
                return false;
            }

            email = claims.getSubject();
            if (email == null || email.isBlank()) {
                writeError(request, response, 401, "Token JWT invalide ou expiré");
                return false;
            }
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Token JWT invalide : {}", e.getMessage());
            writeError(request, response, 401, "Token JWT invalide ou expiré");
            return false;
        }

        // 1. Si le token contient déjà un claim userId ou id (ex: token de test autonome pour Swagger),
        // on l'utilise directement sans appel réseau vers commun-service
        Object userIdClaim = claims.get("userId") != null ? claims.get("userId") : claims.get("id");
        if (userIdClaim != null) {
            try {
                Long userId = Long.valueOf(userIdClaim.toString());
                String role = claims.get("role", String.class);
                AuthenticatedUser user = new AuthenticatedUser(userId, email, role != null ? role : "CANDIDAT", token);
                request.setAttribute(AUTHENTICATED_USER_ATTR, user);
                return true;
            } catch (NumberFormatException ignored) {}
        }

        // 2. Sinon, résolution de l'id numérique via commun-service /auth/me (cache 10 min).\n        // Retirer ce fallback lorsque commun-service ajoutera id/userId dans le JWT.
        try {
            AuthenticatedUser user = communClient.resolveUser(email, token);
            request.setAttribute(AUTHENTICATED_USER_ATTR, user);
            return true;
        } catch (ServiceUnavailableException e) {
            log.error("commun-service indisponible : {}", e.getMessage());
            writeError(request, response, 503, e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Erreur lors de la résolution de l'utilisateur : {}", e.getMessage(), e);
            writeError(request, response, 503, "commun-service est injoignable : " + e.getMessage());
            return false;
        }
    }

    private void writeError(HttpServletRequest request, HttpServletResponse response, int status, String message) throws IOException {
        addCorsHeaders(request, response);
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(new ApiError(status, message)));
    }

    private void addCorsHeaders(HttpServletRequest request, HttpServletResponse response) {
        String origin = request.getHeader(HttpHeaders.ORIGIN);
        if (origin != null && (origin.matches("^https?://localhost(:\\d+)?$") || origin.matches("^https?://127\\.0\\.0\\.1(:\\d+)?$"))) {
            response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
            response.setHeader(HttpHeaders.VARY, HttpHeaders.ORIGIN);
            response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, PUT, PATCH, DELETE, OPTIONS");
            response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "*");
        }
    }
}
