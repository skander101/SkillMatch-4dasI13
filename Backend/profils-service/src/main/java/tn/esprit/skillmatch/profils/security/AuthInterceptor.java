package tn.esprit.skillmatch.profils.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.servlet.HandlerInterceptor;
import tn.esprit.skillmatch.profils.exception.ApiError;
import tn.esprit.skillmatch.profils.exception.IdentityResolutionException;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtException;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final IdentityResolver identityResolver;
    private final ObjectMapper objectMapper;

    public AuthInterceptor(IdentityResolver identityResolver, ObjectMapper objectMapper) {
        this.identityResolver = identityResolver;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            writeError(response, 401, "Authentification requise");
            return false;
        }
        try {
            IdentityResolver.Identity identity = identityResolver.resolve(header.substring(7));
            request.setAttribute("userId", identity.userId());
            request.setAttribute("email", identity.email());
            request.setAttribute("role", identity.role());
            return true;
        } catch (JwtException | RestClientException | IdentityResolutionException exception) {
            writeError(response, 401, "Token JWT invalide ou identité introuvable");
            return false;
        }
    }

    private void writeError(HttpServletResponse response, int status, String message) throws Exception {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), new ApiError(status, message));
    }
}
