package tn.esprit.skillmatch.entreprises.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;
import tn.esprit.skillmatch.entreprises.client.UtilisateurClient;
import tn.esprit.skillmatch.entreprises.client.UtilisateurInfo;

import java.util.List;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String USER_ATTRIBUTE = "authenticatedUser";

    private static final String BEARER_PREFIX = "Bearer ";
    private static final List<String> PUBLISH_ROLES = List.of("CANDIDAT", "RECRUTEUR");
    private static final List<String> ADMIN_ROLES = List.of("ADMIN");

    private final UtilisateurClient utilisateurClient;

    public AuthInterceptor(UtilisateurClient utilisateurClient) {
        this.utilisateurClient = utilisateurClient;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (isPublicRead(request)) {
            return true;
        }

        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX) || authorization.length() == BEARER_PREFIX.length()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentification requise");
        }

        UtilisateurInfo utilisateur = utilisateurClient.currentUser(authorization);
        request.setAttribute(USER_ATTRIBUTE, utilisateur);

        if (isAvisSubmission(request)) {
            requireRole(utilisateur, PUBLISH_ROLES);
        } else {
            requireRole(utilisateur, ADMIN_ROLES);
        }
        return true;
    }

    private boolean isPublicRead(HttpServletRequest request) {
        return "GET".equalsIgnoreCase(request.getMethod())
                && request.getRequestURI().startsWith("/skillmatch/entreprises");
    }

    private boolean isAvisSubmission(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod()) && request.getRequestURI().endsWith("/avis");
    }

    private void requireRole(UtilisateurInfo utilisateur, List<String> allowedRoles) {
        if (utilisateur.role() == null || allowedRoles.stream().noneMatch(role -> role.equalsIgnoreCase(utilisateur.role()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé pour ce rôle");
        }
    }
}
