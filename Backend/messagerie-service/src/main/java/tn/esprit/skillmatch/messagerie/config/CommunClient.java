package tn.esprit.skillmatch.messagerie.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tn.esprit.skillmatch.messagerie.dto.MeResponse;
import tn.esprit.skillmatch.messagerie.exception.ServiceUnavailableException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client HTTP vers commun-service GET /skillmatch/auth/me.
 * Cache en mémoire (ConcurrentHashMap, TTL 10 min, clé = email).
 * Aucune dépendance externe ajoutée (java.net.http standard JDK 11+).
 */
@Component
public class CommunClient {

    private static final Logger log = LoggerFactory.getLogger(CommunClient.class);
    private static final long CACHE_TTL_MS = 10 * 60 * 1000L; // 10 minutes

    private final String communServiceUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    @org.springframework.beans.factory.annotation.Autowired
    public CommunClient(
            @Value("${commun.service.url}") String communServiceUrl,
            ObjectMapper objectMapper
    ) {
        this(
                communServiceUrl,
                objectMapper,
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(5))
                        .build()
        );
    }

    CommunClient(String communServiceUrl, ObjectMapper objectMapper, HttpClient httpClient) {
        this.communServiceUrl = communServiceUrl;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    /**
     * Résout l'id numérique d'un utilisateur à partir de son token JWT.
     * Vérifie le cache d'abord (clé = email, TTL 10 min).
     *
     * @param email  email extrait du JWT (sub)
     * @param token  le JWT complet (sans "Bearer ")
     * @return AuthenticatedUser contenant id, email et role
     * @throws ServiceUnavailableException si commun-service est injoignable ou retourne une erreur
     */
    public AuthenticatedUser resolveUser(String email, String token) {
        // Vérifier le cache
        CacheEntry cached = cache.get(email);
        if (cached != null && !cached.isExpired()) {
            // Attacher le JWT courant à l'identité résolue.
            return new AuthenticatedUser(cached.user().id(), cached.user().email(), cached.user().role(), token);
        }

        // Appel HTTP vers commun-service
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(communServiceUrl + "/skillmatch/auth/me"))
                    .header("Authorization", "Bearer " + token)
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("commun-service /auth/me a retourné le code {}", response.statusCode());
                throw new ServiceUnavailableException(
                        "Impossible de résoudre l'utilisateur : commun-service a retourné " + response.statusCode());
            }

            MeResponse meResponse = objectMapper.readValue(response.body(), MeResponse.class);
            AuthenticatedUser cachedIdentity = new AuthenticatedUser(meResponse.id(), meResponse.email(), meResponse.role());

            // Mettre en cache l'identité (sans figer un ancien JWT) et retourner avec le token courant
            cache.put(email, new CacheEntry(cachedIdentity, System.currentTimeMillis()));
            return new AuthenticatedUser(meResponse.id(), meResponse.email(), meResponse.role(), token);

        } catch (ServiceUnavailableException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erreur lors de l'appel à commun-service /auth/me : {}", e.getMessage(), e);
            throw new ServiceUnavailableException(
                    "commun-service est injoignable : " + e.getMessage());
        }
    }

    /** Vide le cache (utile pour les tests). */
    public void clearCache() {
        cache.clear();
    }

    private record CacheEntry(AuthenticatedUser user, long createdAt) {
        boolean isExpired() {
            return System.currentTimeMillis() - createdAt > CACHE_TTL_MS;
        }
    }
}
