package tn.esprit.skillmatch.alertes.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import tn.esprit.skillmatch.alertes.exception.ApiError;

import java.io.IOException;

/**
 * Même principe que AdminInterceptor de commun-service, pour les routes du back office
 * (/skillmatch/alerts/admin/** et /skillmatch/favorites/admin/**) :
 * JWT obligatoire (401), rôle ADMIN obligatoire (403).
 */
@Component
public class AdminInterceptor implements HandlerInterceptor {

    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    public AdminInterceptor(JwtService jwtService, ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true; // requête CORS préliminaire
        }
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ") || authorization.length() <= 7) {
            writeError(response, 401, "Authentification requise");
            return false;
        }

        Claims claims;
        try {
            claims = jwtService.parseClaims(authorization.substring(7));
        } catch (JwtException | IllegalArgumentException exception) {
            writeError(response, 401, "Token JWT invalide ou expiré");
            return false;
        }

        String role = claims.get("role", String.class);
        if (!"ADMIN".equals(role)) {
            writeError(response, 403, "Accès réservé aux administrateurs");
            return false;
        }
        return true;
    }

    private void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(new ApiError(status, message)));
    }
}
