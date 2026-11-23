package tn.esprit.skillmatch.messagerie.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommunClientTest {

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> httpResponse;

    private CommunClient communClient;

    @BeforeEach
    void setUp() {
        communClient = new CommunClient("http://localhost:8087", new ObjectMapper(), httpClient);
    }

    @Test
    @DisplayName("resolveUser — conserve le token JWT sur le premier appel et lors du hit cache")
    @SuppressWarnings("unchecked")
    void resolveUser_preservesTokenOnFetchAndCacheHit() throws Exception {
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("{\"id\":1,\"nom\":\"Meriem Attia\",\"email\":\"meriem@test.com\",\"role\":\"CANDIDAT\"}");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);

        AuthenticatedUser first = communClient.resolveUser("meriem@test.com", "jwt-token-1");
        assertEquals(1L, first.id());
        assertEquals("meriem@test.com", first.email());
        assertEquals("CANDIDAT", first.role());
        assertEquals("jwt-token-1", first.token());

        // Deuxième appel dans le TTL cache avec un nouveau token → cache hit mais nouveau token conservé
        AuthenticatedUser cached = communClient.resolveUser("meriem@test.com", "jwt-token-2");
        assertEquals(1L, cached.id());
        assertEquals("jwt-token-2", cached.token());
        verify(httpClient, times(1)).send(any(HttpRequest.class), any());
    }

}
