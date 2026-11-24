package tn.esprit.skillmatch.profils.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tn.esprit.skillmatch.profils.exception.IdentityResolutionException;

@Component
public class IdentityResolver {

    private final JwtService jwtService;
    private final RestClient communClient;

    public IdentityResolver(JwtService jwtService,
                            @Value("${commun-service.url:${GATEWAY_URL:http://localhost:8080}}") String communUrl) {
        this.jwtService = jwtService;
        this.communClient = RestClient.builder().baseUrl(communUrl).build();
    }

    public Identity resolve(String token) {
        Long claimUserId = jwtService.extractUserId(token);
        String email = jwtService.extractEmail(token);
        String role = jwtService.extractRole(token);
        if (claimUserId != null && email != null && role != null) {
            return new Identity(claimUserId, email, role);
        }
        if (email == null) {
            throw new IdentityResolutionException("Le token JWT ne contient pas d'email");
        }
        UserResponse user = communClient.get()
                .uri("/skillmatch/auth/me")
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .body(UserResponse.class);
        if (user == null || user.id() == null) {
            throw new IdentityResolutionException("Identité utilisateur introuvable");
        }
        return new Identity(user.id(), user.email(), user.role() == null ? role : user.role());
    }

    public record Identity(Long userId, String email, String role) {
    }

    private record UserResponse(Long id, String email, String role) {
    }
}
