package tn.esprit.skillmatch.tests.security;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
class SecuriteTests {

    @Bean
    SecurityFilterChain chaine(HttpSecurity http) throws Exception {
        var bearer = new BearerTokenAuthenticationEntryPoint();
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(routes -> routes
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/tests/*/submit").hasRole("CANDIDAT")
                        .requestMatchers(HttpMethod.POST, "/api/tests").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/tests/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/tests/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/tests", "/api/tests/**").authenticated()
                        .anyRequest().denyAll())
                .oauth2ResourceServer(oauth -> oauth
                        .opaqueToken(Customizer.withDefaults())
                        .authenticationEntryPoint((request, response, exception) -> {
                            if (exception instanceof AuthenticationServiceException) {
                                response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
                            } else {
                                bearer.commence(request, response, exception);
                            }
                        }))
                .build();
    }
}
