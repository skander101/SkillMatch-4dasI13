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
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserClientTest {

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> httpResponse;

    private UserClient userClient;

    @BeforeEach
    void setUp() {
        userClient = new UserClient("http://localhost:8087", new ObjectMapper(), httpClient);
    }

    @Test
    @DisplayName("resolveUsers — résout un lot via /batch puis sert depuis le cache")
    @SuppressWarnings("unchecked")
    void resolveUsers_batchAndCache() throws Exception {
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(
                "[{\"id\":1,\"nom\":\"Attia Ben\",\"role\":\"CANDIDAT\"},{\"id\":2,\"nom\":\"Meriem K.\",\"role\":\"RECRUTEUR\"}]"
        );
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);

        Map<Long, UserClient.UserProfile> first = userClient.resolveUsers(List.of(1L, 2L), "jwt-token");
        assertEquals(2, first.size());
        assertEquals("Attia Ben", first.get(1L).nom());
        assertEquals("RECRUTEUR", first.get(2L).role());

        // Deuxième appel dans le TTL cache : aucun nouvel appel HTTP
        Map<Long, UserClient.UserProfile> second = userClient.resolveUsers(List.of(1L, 2L), "jwt-token");
        assertEquals(2, second.size());
        assertEquals("Meriem K.", second.get(2L).nom());
        assertEquals("RECRUTEUR", userClient.resolveRole(2L, "jwt-token"));
        verify(httpClient, times(1)).send(any(HttpRequest.class), any());
    }
}
