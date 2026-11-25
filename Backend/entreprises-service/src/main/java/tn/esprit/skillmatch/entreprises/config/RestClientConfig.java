package tn.esprit.skillmatch.entreprises.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient utilisateurRestClient(@Value("${commun-service.url}") String communServiceUrl) {
        return RestClient.builder()
                .baseUrl(communServiceUrl)
                .build();
    }
}
