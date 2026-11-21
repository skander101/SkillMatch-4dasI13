package tn.esprit.skillmatch.commun.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import tn.esprit.skillmatch.commun.exception.ApiError;

import java.io.IOException;

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
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ") || authorization.length() <= 7) {
            writeError(response, 401, "Authentification requise");
            return false;
        }

        String token = authorization.substring(7);
        try {
            jwtService.extractEmail(token);
            String role = jwtService.extractRole(token);
            if (!"ADMIN".equals(role)) {
                writeError(response, 403, "Accès réservé aux administrateurs");
                return false;
            }
        } catch (JwtException | IllegalArgumentException exception) {
            writeError(response, 401, "Token JWT invalide ou expiré");
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
