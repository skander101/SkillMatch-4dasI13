package tn.esprit.skillmatch.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ConfigServiceTest {
    @Autowired MockMvc mvc;

    @Test
    void fournitSeulementLaConfigurationOffresSansSecrets() throws Exception {
        String json = mvc.perform(get("/offres-service/default")).andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("offres-service"))
                .andExpect(jsonPath("$.propertySources[0].source['info.configuration.origine']").value("config-server"))
                .andReturn().getResponse().getContentAsString();
        assertTrue(json.contains("spring.datasource.url"));
        assertTrue(json.contains("spring.rabbitmq.host"));
        assertFalse(json.contains("jwt.secret"));
        assertFalse(json.contains("password"));
        mvc.perform(get("/commun-service/default")).andExpect(status().isNotFound());
    }
}
