package tn.esprit.skillmatch.alertes.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;
import tn.esprit.skillmatch.alertes.client.CommunClient;
import tn.esprit.skillmatch.alertes.exception.ApiError;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Même principe que AdminInterceptor de commun-service :
 * JWT obligatoire (401), rôle CANDIDAT obligatoire (403), puis l'utilisateur est placé dans la requête.
 */
@Component
public class CandidatInterceptor implements HandlerInterceptor {

    private final JwtService jwtService;
    private final CommunClient communClient;
    private final ObjectMapper objectMapper;
    /** email -> id, pour ne pas rappeler commun-service à chaque requête. */
    private final Map<String, Long> userIds = new ConcurrentHashMap<>();

    public CandidatInterceptor(JwtService jwtService, CommunClient communClient, ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.communClient = communClient;
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

        String email = claims.getSubject();
        String role = claims.get("role", String.class);
        if (!"CANDIDAT".equals(role)) {
            writeError(response, 403, "Accès réservé aux candidats");
            return false;
        }

        Long userId;
        try {
            userId = resolveUserId(claims, email, authorization);
        } catch (ResponseStatusException exception) {
            writeError(response, exception.getStatusCode().value(), exception.getReason());
            return false;
        }

        request.setAttribute(CurrentUser.REQUEST_ATTRIBUTE, new CurrentUser(userId, email, role));
        return true;
    }

    /** Si le JWT contient un jour l'id (claim "userId" ou "id"), on l'utilise ; sinon on interroge commun-service. */
    private Long resolveUserId(Claims claims, String email, String authorization) {
        Object claim = claims.get("userId") != null ? claims.get("userId") : claims.get("id");
        if (claim instanceof Number number) {
            return number.longValue();
        }
        return userIds.computeIfAbsent(email, key -> communClient.fetchUserId(authorization));
    }

    private void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(new ApiError(status, message)));
    }
}
