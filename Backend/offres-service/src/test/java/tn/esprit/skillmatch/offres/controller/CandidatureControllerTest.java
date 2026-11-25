package tn.esprit.skillmatch.offres.controller; // Validation HTTP sans MySQL.

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import com.fasterxml.jackson.databind.*;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.skillmatch.offres.exception.GlobalExceptionHandler;
import tn.esprit.skillmatch.offres.service.CandidatureService;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CandidatureControllerTest {
    private CandidatureService service;
    private MockMvc mvc;

    @BeforeEach
    void preparer() {
        service = mock(CandidatureService.class); // Les règles métier sont testées séparément.
        mvc = MockMvcBuilders.standaloneSetup(new CandidatureController(service))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(new ObjectMapper().findAndRegisterModules().disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)))
                .setControllerAdvice(new GlobalExceptionHandler()).build(); // Même format d'erreur que les offres.
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "POST|{\"offreId\":1,\"lettre\":\"   \",\"cvId\":1}",
            "POST|{\"offreId\":1,\"lettre\":\"Lettre\",\"cvId\":1.5}",
            "POST|{\"offreId\":1.5,\"lettre\":\"Lettre\",\"cvId\":1}",
            "PUT|{\"cvId\":1.5}", "PUT|{\"noteRecruteur\":2.5}",
            "PUT|{\"noteRecruteur\":2147483648}",
            "PUT|{\"dateEntretien\":\"2020-01-01T10:00:00\"}"
    })
    void refuseLaSaisieAvantLeService(String methode, String corps) throws Exception {
        var requete = methode.equals("POST") ? post("/skillmatch/candidatures") : put("/skillmatch/candidatures/1");
        mvc.perform(requete.contentType(MediaType.APPLICATION_JSON).content(corps)).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void refuseLesFormulairesInvalides() throws Exception {
        mvc.perform(post("/skillmatch/candidatures").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()); // Offre, lettre et CV obligatoires.
        mvc.perform(put("/skillmatch/candidatures/1").contentType(MediaType.APPLICATION_JSON).content("{\"cvId\":0,\"noteRecruteur\":-1}"))
                .andExpect(status().isBadRequest()); // Identifiant et note invalides.
        mvc.perform(put("/skillmatch/candidatures/1").contentType(MediaType.APPLICATION_JSON).content("{\"statut\":\"INCONNU\"}"))
                .andExpect(status().isBadRequest()); // Statut hors UML.
        verifyNoInteractions(service); // Le service n'est pas appelé.
    }

    @Test
    void retourne401SansSessionEt404SiAbsente() throws Exception {
        when(service.lister(null, "", "dateCandidatureDesc")).thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentification requise"));
        mvc.perform(get("/skillmatch/candidatures")).andExpect(status().isUnauthorized()); // Session absente.
        when(service.consulter(99L, "Bearer test")).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Candidature introuvable"));
        mvc.perform(get("/skillmatch/candidatures/99").header("Authorization", "Bearer test"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
    }
}
