package tn.esprit.skillmatch.alertes.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import tn.esprit.skillmatch.alertes.security.AdminInterceptor;
import tn.esprit.skillmatch.alertes.security.CandidatInterceptor;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final String[] ADMIN_PATHS = {
            "/skillmatch/alerts/admin", "/skillmatch/alerts/admin/**",
            "/skillmatch/favorites/admin", "/skillmatch/favorites/admin/**"
    };

    private final CandidatInterceptor candidatInterceptor;
    private final AdminInterceptor adminInterceptor;

    public WebConfig(CandidatInterceptor candidatInterceptor, AdminInterceptor adminInterceptor) {
        this.candidatInterceptor = candidatInterceptor;
        this.adminInterceptor = adminInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Back office : rôle ADMIN
        registry.addInterceptor(adminInterceptor)
                .addPathPatterns(ADMIN_PATHS);
        // Front office : rôle CANDIDAT
        registry.addInterceptor(candidatInterceptor)
                .addPathPatterns("/skillmatch/alerts/**", "/skillmatch/favorites/**")
                .excludePathPatterns(ADMIN_PATHS);
    }
}
