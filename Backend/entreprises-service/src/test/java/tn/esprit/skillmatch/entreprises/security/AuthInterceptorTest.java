package tn.esprit.skillmatch.entreprises.security;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.skillmatch.entreprises.client.UtilisateurClient;
import tn.esprit.skillmatch.entreprises.client.UtilisateurInfo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AuthInterceptorTest {

    private final UtilisateurClient utilisateurClient = mock(UtilisateurClient.class);
    private final AuthInterceptor interceptor = new AuthInterceptor(utilisateurClient);

    @Test
    void publicGetIsAllowedWithoutAuthentication() {
        MockHttpServletRequest request = request("GET", "/skillmatch/entreprises");

        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
        verifyNoInteractions(utilisateurClient);
    }

    @Test
    void missingAuthorizationIsRejected() {
        MockHttpServletRequest request = request("POST", "/skillmatch/entreprises/1/avis");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                interceptor.preHandle(request, new MockHttpServletResponse(), new Object())
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        verifyNoInteractions(utilisateurClient);
    }

    @Test
    void candidateCanPublishAvisAndUserIsStoredOnRequest() {
        UtilisateurInfo utilisateur = utilisateur("CANDIDAT");
        when(utilisateurClient.currentUser("Bearer token")).thenReturn(utilisateur);
        MockHttpServletRequest request = authorizedRequest("POST", "/skillmatch/entreprises/1/avis");

        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
        assertSame(utilisateur, request.getAttribute(AuthInterceptor.USER_ATTRIBUTE));
    }

    @Test
    void adminCannotPublishAvis() {
        when(utilisateurClient.currentUser("Bearer token")).thenReturn(utilisateur("ADMIN"));
        MockHttpServletRequest request = authorizedRequest("POST", "/skillmatch/entreprises/1/avis");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                interceptor.preHandle(request, new MockHttpServletResponse(), new Object())
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    void adminCanModerateAvis() {
        when(utilisateurClient.currentUser("Bearer token")).thenReturn(utilisateur("ADMIN"));
        MockHttpServletRequest request = authorizedRequest("PUT", "/skillmatch/avis/2/moderation");

        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
    }

    @Test
    void candidateCannotModerateAvis() {
        when(utilisateurClient.currentUser("Bearer token")).thenReturn(utilisateur("CANDIDAT"));
        MockHttpServletRequest request = authorizedRequest("PUT", "/skillmatch/avis/2/moderation");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                interceptor.preHandle(request, new MockHttpServletResponse(), new Object())
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    void candidateCannotCreateEntreprise() {
        when(utilisateurClient.currentUser("Bearer token")).thenReturn(utilisateur("CANDIDAT"));
        MockHttpServletRequest request = authorizedRequest("POST", "/skillmatch/entreprises");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                interceptor.preHandle(request, new MockHttpServletResponse(), new Object())
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    private MockHttpServletRequest request(String method, String uri) {
        return new MockHttpServletRequest(method, uri);
    }

    private MockHttpServletRequest authorizedRequest(String method, String uri) {
        MockHttpServletRequest request = request(method, uri);
        request.addHeader("Authorization", "Bearer token");
        return request;
    }

    private UtilisateurInfo utilisateur(String role) {
        return new UtilisateurInfo(10L, "Alice Martin", "alice@example.com", role);
    }
}
