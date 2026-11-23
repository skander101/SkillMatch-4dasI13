package tn.esprit.skillmatch.messagerie.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import tn.esprit.skillmatch.messagerie.exception.ServiceUnavailableException;

import javax.crypto.SecretKey;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthInterceptorTest {

    private static final String SECRET = "test-secret-key-for-unit-tests-must-be-at-least-32-bytes-long";

    @Mock
    private CommunClient communClient;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private AuthInterceptor interceptor;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private StringWriter responseWriter;

    @BeforeEach
    void setUp() throws Exception {
        interceptor = new AuthInterceptor(SECRET, communClient, objectMapper);
        responseWriter = new StringWriter();
        lenient().when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
    }

    private String generateValidToken(String email, String role) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600_000))
                .signWith(key)
                .compact();
    }

    @Test
    @DisplayName("Header Authorization absent → 401")
    void missingHeader_returns401() throws Exception {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(null);

        boolean result = interceptor.preHandle(request, response, new Object());

        assertFalse(result);
        verify(response).setStatus(401);
        assertTrue(responseWriter.toString().contains("Authentification requise"));
    }

    @Test
    @DisplayName("Header Authorization sans Bearer → 401")
    void invalidHeaderPrefix_returns401() throws Exception {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Basic dXNlcjpwYXNz");

        boolean result = interceptor.preHandle(request, response, new Object());

        assertFalse(result);
        verify(response).setStatus(401);
        assertTrue(responseWriter.toString().contains("Authentification requise"));
    }

    @Test
    @DisplayName("Token JWT invalide → 401")
    void invalidToken_returns401() throws Exception {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer invalid.token.value");

        boolean result = interceptor.preHandle(request, response, new Object());

        assertFalse(result);
        verify(response).setStatus(401);
        assertTrue(responseWriter.toString().contains("Token JWT invalide ou expiré"));
    }

    @Test
    @DisplayName("Token JWT valide + résolution utilisateur réussie → 200 (true) + attribut renseigné")
    void validToken_success() throws Exception {
        String token = generateValidToken("candidat@test.com", "CANDIDAT");
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer " + token);

        AuthenticatedUser user = new AuthenticatedUser(1L, "candidat@test.com", "CANDIDAT");
        when(communClient.resolveUser("candidat@test.com", token)).thenReturn(user);

        boolean result = interceptor.preHandle(request, response, new Object());

        assertTrue(result);
        verify(request).setAttribute(AuthInterceptor.AUTHENTICATED_USER_ATTR, user);
    }

    @Test
    @DisplayName("JWT sans claim exp → 401")
    void tokenWithoutExpiration_returns401() throws Exception {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .subject("candidat@test.com")
                .claim("role", "CANDIDAT")
                .issuedAt(new Date())
                .signWith(key)
                .compact();
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer " + token);

        boolean result = interceptor.preHandle(request, response, new Object());

        assertFalse(result);
        verify(response).setStatus(401);
    }

    @Test
    @DisplayName("commun-service indisponible → 503")
    void communServiceUnavailable_returns503() throws Exception {
        String token = generateValidToken("candidat@test.com", "CANDIDAT");
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer " + token);

        when(communClient.resolveUser("candidat@test.com", token))
                .thenThrow(new ServiceUnavailableException("commun-service est injoignable"));

        boolean result = interceptor.preHandle(request, response, new Object());

        assertFalse(result);
        verify(response).setStatus(503);
        assertTrue(responseWriter.toString().contains("503"));
    }

    @Test
    @DisplayName("Erreur 401 avec Origin localhost → en-têtes CORS ajoutés")
    void missingHeader_withLocalhostOrigin_addsCorsHeaders() throws Exception {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(null);
        when(request.getHeader(HttpHeaders.ORIGIN)).thenReturn("http://localhost:4200");

        boolean result = interceptor.preHandle(request, response, new Object());

        assertFalse(result);
        verify(response).setStatus(401);
        verify(response).setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:4200");
        verify(response).setHeader(HttpHeaders.VARY, HttpHeaders.ORIGIN);
    }

    @Test
    @DisplayName("Erreur 503 avec Origin 127.0.0.1 → en-têtes CORS ajoutés")
    void communServiceUnavailable_withLoopbackOrigin_addsCorsHeaders() throws Exception {
        String token = generateValidToken("candidat@test.com", "CANDIDAT");
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer " + token);
        when(request.getHeader(HttpHeaders.ORIGIN)).thenReturn("http://127.0.0.1:4200");
        when(communClient.resolveUser("candidat@test.com", token))
                .thenThrow(new ServiceUnavailableException("commun-service est injoignable"));

        boolean result = interceptor.preHandle(request, response, new Object());

        assertFalse(result);
        verify(response).setStatus(503);
        verify(response).setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://127.0.0.1:4200");
    }
}
