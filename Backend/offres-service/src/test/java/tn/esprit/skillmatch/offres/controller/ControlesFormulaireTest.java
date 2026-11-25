package tn.esprit.skillmatch.offres.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tn.esprit.skillmatch.offres.entity.*;
import tn.esprit.skillmatch.offres.repository.*;
import tn.esprit.skillmatch.offres.security.IdentiteService;
import tn.esprit.skillmatch.offres.security.IdentiteService.Identite;
import tn.esprit.skillmatch.offres.service.CandidatureService;
import tn.esprit.skillmatch.offres.exception.GlobalExceptionHandler;
import java.time.LocalDate;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ControlesFormulaireTest {
    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "RECRUTEUR", "CANDIDAT"})
    void modificationValideEtErreursSontControleesPourChaqueRole(String role) throws Exception {
        var repository = mock(CandidatureRepository.class);
        var identites = mock(IdentiteService.class);
        when(identites.courante("Bearer test")).thenReturn(new Identite(1L, "Test", "test@example.invalid", role));
        Offre offre = new Offre(); offre.setId(2L); offre.setRecruteurId(1L);
        Candidature c = new Candidature(); c.setOffre(offre); c.setUserId(1L); c.setCvId(null);
        c.setLettre("Je souhaite rejoindre votre équipe de développement.");
        when(repository.findById(1L)).thenReturn(Optional.of(c));
        when(repository.save(c)).thenReturn(c);
        var service = new CandidatureService(repository, mock(OffreRepository.class), identites, mock(ApplicationEventPublisher.class), new tn.esprit.skillmatch.offres.service.OffreService(null, null, null, null));
        var json = new ObjectMapper().findAndRegisterModules();
        var mvc = MockMvcBuilders.standaloneSetup(new CandidatureController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(json)).build();
        Map<String, Object> valide = new HashMap<>();
        if (!role.equals("RECRUTEUR")) { valide.put("lettre", c.getLettre()); }
        if (!role.equals("CANDIDAT")) {
            valide.put("dateEntretien", LocalDate.now().atStartOfDay()); // Aujourd'hui est accepté, même si l'heure est passée.
            valide.put("noteRecruteur", 0); valide.put("commentaireRecruteur", "Profil intéressant");
        }
        String champ = role.equals("RECRUTEUR") ? "commentaireRecruteur" : "lettre";
        for (String texte : List.of("", "   ", "abc", "a".repeat(10001))) {
            var invalide = new HashMap<>(valide); invalide.put(champ, texte);
            mvc.perform(put("/skillmatch/candidatures/1").header("Authorization", "Bearer test")
                    .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(invalide))).andExpect(status().isBadRequest());
        }
        if (!role.equals("CANDIDAT")) {
            var invalide = new HashMap<>(valide); invalide.remove("noteRecruteur");
            mvc.perform(put("/skillmatch/candidatures/1").header("Authorization", "Bearer test")
                    .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(invalide))).andExpect(status().isBadRequest());
            invalide = new HashMap<>(valide); invalide.put("dateEntretien", LocalDate.now().minusDays(1).atStartOfDay());
            mvc.perform(put("/skillmatch/candidatures/1").header("Authorization", "Bearer test")
                    .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(invalide))).andExpect(status().isBadRequest());
        }
        verify(repository, never()).save(any());
        mvc.perform(put("/skillmatch/candidatures/1").header("Authorization", "Bearer test")
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(valide))).andExpect(status().isOk());
        verify(repository).save(c);
    }
}
