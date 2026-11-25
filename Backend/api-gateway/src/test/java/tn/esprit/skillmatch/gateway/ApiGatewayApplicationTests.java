package tn.esprit.skillmatch.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class ApiGatewayApplicationTests {

    @Autowired
    private RouteLocator routeLocator;

    @Test
    void contextLoads() {
    }

    @Test
    void entreprisesRoutesAreConfigured() {
        List<String> routeIds = routeLocator.getRoutes()
                .map(Route::getId)
                .collectList()
                .block();

        assertNotNull(routeIds);
        assertTrue(routeIds.containsAll(List.of("entreprises", "avis")));
    }
}
