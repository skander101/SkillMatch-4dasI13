package tn.esprit.skillmatch.tests.security;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.introspection.BadOpaqueTokenException;
import org.springframework.security.oauth2.server.resource.introspection.OAuth2IntrospectionException;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
class ClientIdentite implements OpaqueTokenIntrospector {

    private static final Set<String> ROLES = Set.of("CANDIDAT", "RECRUTEUR", "ADMIN");
    private final RestClient commun;

    ClientIdentite(RestClient.Builder builder, @Value("${app.commun.url}") String adresse) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(2));
        commun = builder.baseUrl(adresse).requestFactory(factory).build();
    }

    @Override
    public OAuth2AuthenticatedPrincipal introspect(String token) {
        Compte compte;
        try {
            compte = commun.get()
                    .uri("/skillmatch/auth/me")
                    .headers(headers -> headers.setBearerAuth(token))
                    .retrieve()
                    .body(Compte.class);
        } catch (HttpClientErrorException exception) {
            throw new BadOpaqueTokenException("Jeton refusé par le service commun.");
        } catch (RestClientException exception) {
            throw new OAuth2IntrospectionException("Service commun indisponible.", exception);
        }
        if (compte == null || compte.id() == null || compte.id() <= 0
                || compte.role() == null || !ROLES.contains(compte.role())) {
            throw new BadOpaqueTokenException("Identité SkillMatch invalide.");
        }
        return new DefaultOAuth2AuthenticatedPrincipal(
                compte.id().toString(),
                Map.of("userId", compte.id(), "role", compte.role()),
                List.of(new SimpleGrantedAuthority("ROLE_" + compte.role())));
    }

    private record Compte(Long id, String role) {
    }
}
