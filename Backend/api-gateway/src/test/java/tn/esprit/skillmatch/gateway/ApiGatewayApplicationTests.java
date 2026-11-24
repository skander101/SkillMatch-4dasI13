package tn.esprit.skillmatch.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "eureka.client.enabled=false") // Ce test de contexte reste indépendant du serveur Eureka.
class ApiGatewayApplicationTests {

    @Test
    void contextLoads() {
    }
}
