package tn.esprit.skillmatch.tests;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("verification")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DemarrageServiceTest {

    @Autowired
    private TestRestTemplate http;

    @Test
    void leServiceDemarreEtExposeSaSante() {
        var reponse = http.getForEntity("/actuator/health", String.class);
        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(reponse.getBody()).contains("\"status\":\"UP\"");
    }
}
