package tn.esprit.skillmatch.entreprises.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CommunUtilisateurClientTest {

    private MockRestServiceServer server;
    private UtilisateurClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:8087");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new CommunUtilisateurClient(builder.build());
    }

    @Test
    void currentUserCallsAuthMeAndForwardsAuthorization() {
        server.expect(requestTo("http://localhost:8087/skillmatch/auth/me"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer token"))
                .andRespond(withSuccess(
                        "{\"id\":10,\"nom\":\"Alice Martin\",\"email\":\"alice@example.com\",\"role\":\"CANDIDAT\"}",
                        MediaType.APPLICATION_JSON
                ));

        UtilisateurInfo utilisateur = client.currentUser("Bearer token");

        assertEquals(10L, utilisateur.id());
        assertEquals("Alice Martin", utilisateur.nom());
        assertEquals("alice@example.com", utilisateur.email());
        assertEquals("CANDIDAT", utilisateur.role());
        server.verify();
    }

    @Test
    void unauthorizedResponseBecomesUnauthorized() {
        server.expect(requestTo("http://localhost:8087/skillmatch/auth/me"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                client.currentUser("Bearer token")
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        server.verify();
    }

    @Test
    void unreachableCommunServiceBecomesServiceUnavailable() {
        ResponseCreator unavailable = request -> {
            throw new ResourceAccessException("commun-service down");
        };
        server.expect(requestTo("http://localhost:8087/skillmatch/auth/me"))
                .andRespond(unavailable);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                client.currentUser("Bearer token")
        );

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatusCode());
        server.verify();
    }
}
