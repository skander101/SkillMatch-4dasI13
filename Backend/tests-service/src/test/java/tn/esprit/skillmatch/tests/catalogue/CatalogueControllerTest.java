package tn.esprit.skillmatch.tests.catalogue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@DataJpaTest(showSql = false)
@Import(CatalogueTests.class)
class CatalogueControllerTest {

    @Autowired
    private CatalogueTests catalogue;

    @Test
    void creeEtConsulteUnTestSansPublierLaCorrection() throws Exception {
        MockMvc mvc = standaloneSetup(new CatalogueController(catalogue)).build();
        String emplacement = mvc.perform(post("/api/tests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "titre": "Java",
                                  "competence": "Java",
                                  "dureeMinutes": 20,
                                  "seuilReussite": 70,
                                  "questions": [{
                                    "id": "q1",
                                    "enonce": "Que signifie JVM ?",
                                    "options": ["Java Virtual Machine", "Java Version Manager"],
                                    "indexBonneReponse": 0
                                  }]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.questions[0].indexBonneReponse").doesNotExist())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andReturn().getResponse().getHeader(HttpHeaders.LOCATION);

        mvc.perform(get(emplacement))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions[0].id").value("q1"))
                .andExpect(jsonPath("$.questions[0].options[0]").value("Java Virtual Machine"))
                .andExpect(jsonPath("$.questions[0].indexBonneReponse").doesNotExist());
    }
}
