package tn.esprit.skillmatch.messagerie.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tn.esprit.skillmatch.messagerie.exception.ServiceUnavailableException;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client lecture seule vers l'annuaire de commun-service.
 * Utilise un cache mémoire à durée courte (3 minutes) pour résoudre noms et rôles.
 * Aucun autre microservice n'est modifié.
 */
@Component
public class UserClient {

    private static final Logger log = LoggerFactory.getLogger(UserClient.class);
    private static final long CONNECT_TIMEOUT_SECONDS = 5;
    private static final long CACHE_TTL_MS = 3 * 60 * 1000L; // 3 minutes

    private final String communServiceUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<Long, CacheEntry> cache = new ConcurrentHashMap<>();

    @Autowired
    public UserClient(
            @Value("${commun.service.url}") String communServiceUrl,
            ObjectMapper objectMapper
    ) {
        this(
                communServiceUrl,
                objectMapper,
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(CONNECT_TIMEOUT_SECONDS))
                        .build()
        );
    }

    UserClient(String communServiceUrl, ObjectMapper objectMapper, HttpClient httpClient) {
        this.communServiceUrl = communServiceUrl;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    /**
     * Résout le rôle d'un utilisateur via l'annuaire existant (avec cache courte durée).
     *
     * @param userId identifiant utilisateur
     * @param token  JWT complet (sans "Bearer ")
     * @return le rôle (ex. "CANDIDAT", "RECRUTEUR", "ADMIN") ou null si introuvable
     * @throws ServiceUnavailableException si l'annuaire est injoignable
     */
    public String resolveRole(Long userId, String token) {
        return resolveUser(userId, token)
                .map(UserProfile::role)
                .orElse(null);
    }

    /**
     * Résout le profil public (id, nom, rôle) d'un utilisateur par son identifiant.
     */
    public Optional<UserProfile> resolveUser(Long userId, String token) {
        if (userId == null || userId <= 0) {
            return Optional.empty();
        }
        CacheEntry cached = cache.get(userId);
        if (cached != null && !cached.isExpired()) {
            return Optional.of(cached.profile());
        }

        try {
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(communServiceUrl + "/skillmatch/annuaire/utilisateurs/" + userId))
                    .timeout(Duration.ofSeconds(CONNECT_TIMEOUT_SECONDS))
                    .GET();
            if (token != null && !token.isBlank()) {
                reqBuilder.header("Authorization", "Bearer " + token);
            }

            HttpResponse<String> response = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 404) {
                return Optional.empty();
            }
            if (response.statusCode() != 200) {
                throw new ServiceUnavailableException(
                        "Annuaire indisponible (HTTP " + response.statusCode() + ")");
            }

            AnnuaireUser user = objectMapper.readValue(response.body(), AnnuaireUser.class);
            UserProfile profile = toProfile(user);
            cache.put(userId, new CacheEntry(profile, System.currentTimeMillis()));
            return Optional.of(profile);

        } catch (ServiceUnavailableException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceUnavailableException(
                    "Impossible de résoudre le rôle de l'utilisateur : " + e.getMessage());
        }
    }

    /**
     * Résout un lot d'utilisateurs via POST /skillmatch/annuaire/utilisateurs/batch (avec cache courte durée).
     * Utilisé pour enrichir les listes de conversations/messages sans bloquer si l'annuaire est momentanément indisponible.
     */
    public Map<Long, UserProfile> resolveUsers(Collection<Long> userIds, String token) {
        Map<Long, UserProfile> resolved = new LinkedHashMap<>();
        if (userIds == null || userIds.isEmpty()) {
            return resolved;
        }

        List<Long> missing = new ArrayList<>();
        for (Long id : userIds) {
            if (id == null || id <= 0 || resolved.containsKey(id)) {
                continue;
            }
            CacheEntry cached = cache.get(id);
            if (cached != null && !cached.isExpired()) {
                resolved.put(id, cached.profile());
            } else {
                missing.add(id);
            }
        }

        if (missing.isEmpty() || token == null || token.isBlank()) {
            return resolved;
        }

        try {
            String body = objectMapper.writeValueAsString(Map.of("ids", missing));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(communServiceUrl + "/skillmatch/annuaire/utilisateurs/batch"))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(CONNECT_TIMEOUT_SECONDS))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200 && response.body() != null) {
                List<AnnuaireUser> users = objectMapper.readValue(
                        response.body(), new TypeReference<List<AnnuaireUser>>() {});
                long now = System.currentTimeMillis();
                for (AnnuaireUser user : users) {
                    if (user != null && user.id() != null) {
                        UserProfile profile = toProfile(user);
                        cache.put(user.id(), new CacheEntry(profile, now));
                        resolved.put(user.id(), profile);
                    }
                }
            } else {
                log.warn("Annuaire batch a retourné HTTP {}", response.statusCode());
            }
        } catch (Exception e) {
            log.warn("Impossible d'enrichir les participants depuis l'annuaire : {}", e.getMessage());
        }

        return resolved;
    }

    /**
     * Recherche des utilisateurs par nom via l'annuaire existant (10 résultats max).
     */
    public List<UserProfile> searchUsers(String query, String role, String token) {
        if (query == null || query.trim().length() < 2) {
            return List.of();
        }
        try {
            StringBuilder url = new StringBuilder(communServiceUrl)
                    .append("/skillmatch/annuaire/utilisateurs?q=")
                    .append(URLEncoder.encode(query.trim(), StandardCharsets.UTF_8))
                    .append("&limit=10");
            if (role != null && !role.isBlank() && !"ALL".equalsIgnoreCase(role)) {
                url.append("&role=").append(URLEncoder.encode(role.trim(), StandardCharsets.UTF_8));
            }
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url.toString()))
                    .timeout(Duration.ofSeconds(CONNECT_TIMEOUT_SECONDS))
                    .GET();
            if (token != null && !token.isBlank()) {
                reqBuilder.header("Authorization", "Bearer " + token);
            }
            HttpResponse<String> response = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200 || response.body() == null) {
                throw new ServiceUnavailableException("Annuaire indisponible (HTTP " + response.statusCode() + ")");
            }
            List<AnnuaireUser> users = objectMapper.readValue(
                    response.body(), new TypeReference<List<AnnuaireUser>>() {});
            long now = System.currentTimeMillis();
            List<UserProfile> result = new ArrayList<>();
            for (AnnuaireUser u : users) {
                if (u != null && u.id() != null) {
                    UserProfile profile = toProfile(u);
                    cache.put(u.id(), new CacheEntry(profile, now));
                    result.add(profile);
                }
            }
            return result;
        } catch (ServiceUnavailableException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceUnavailableException("Impossible de rechercher dans l'annuaire : " + e.getMessage());
        }
    }

    /** Vide le cache (utile pour les tests). */
    public void clearCache() {
        cache.clear();
    }

    private static UserProfile toProfile(AnnuaireUser user) {
        String nom = user.nom() != null && !user.nom().isBlank() ? user.nom().trim() : "Membre";
        String role = user.role() != null ? user.role().name() : null;
        return new UserProfile(user.id(), nom, role);
    }

    public record UserProfile(Long id, String nom, String role) {
        public String firstName() {
            if (nom == null || nom.isBlank()) {
                return "Membre";
            }
            String[] parts = nom.trim().split("\\s+");
            return parts[0];
        }
    }

    private record AnnuaireUser(Long id, String nom, Role role) {}

    private record CacheEntry(UserProfile profile, long createdAt) {
        boolean isExpired() {
            return System.currentTimeMillis() - createdAt > CACHE_TTL_MS;
        }
    }

    public enum Role {
        CANDIDAT, RECRUTEUR, ADMIN
    }
}
