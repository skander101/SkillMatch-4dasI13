package tn.esprit.skillmatch.alertes.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import tn.esprit.skillmatch.alertes.security.CandidatInterceptor;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final CandidatInterceptor candidatInterceptor;

    public WebConfig(CandidatInterceptor candidatInterceptor) {
        this.candidatInterceptor = candidatInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(candidatInterceptor)
                .addPathPatterns("/skillmatch/alerts/**", "/skillmatch/favorites/**");
    }
}
