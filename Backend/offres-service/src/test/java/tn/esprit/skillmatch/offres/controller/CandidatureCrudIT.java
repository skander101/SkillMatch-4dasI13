package tn.esprit.skillmatch.offres.controller; // Test du CRUD Candidature avec MySQL.

import com.fasterxml.jackson.databind.ObjectMapper; // Lit l'identifiant JSON.
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc; // Routes HTTP sans ouvrir de port.
import org.springframework.transaction.annotation.Transactional; // Annule les données de test.
import tn.esprit.skillmatch.offres.entity.Offre;
import tn.esprit.skillmatch.offres.enums.*;
import tn.esprit.skillmatch.offres.repository.*;
import tn.esprit.skillmatch.offres.security.IdentiteService;
import tn.esprit.skillmatch.offres.security.IdentiteService.Identite;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "eureka.client.enabled=false") // Ne s'inscrit pas dans l'annuaire pendant le test.
@AutoConfigureMockMvc
@Transactional
class CandidatureCrudIT {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired OffreRepository offres;
    @Autowired CandidatureRepository candidatures;
    @MockBean IdentiteService identites; // Pas de vrai JWT ni de compte créé.

    @Test
    void creerLireModifierRetirerPuisSupprimer() throws Exception {
        Offre offre = new Offre(); // Offre de test ouverte.
        offre.setTitre("Test candidature"); offre.setContrat(ContratType.CDI);
        offre.setRecruteurId(1L); offre.setStatut(OffreStatut.PUBLIEE);
        offre.setDateExpiration(LocalDate.now().plusDays(10));
        offres.saveAndFlush(offre);
        when(identites.courante("Bearer test")).thenReturn(new Identite(2L, "Test", "test@example.invalid", "CANDIDAT"));
        String body = "{\"offreId\":" + offre.getId() + ",\"lettre\":\"Je suis motivé pour rejoindre votre équipe Java.\"}";
        mvc.perform(post("/skillmatch/candidatures").header("Authorization", "Bearer test").contentType(MediaType.APPLICATION_JSON).content(body.replace("}", ",\"cvId\":1.5}")))
                .andExpect(status().isBadRequest()); // Vérifie la vraie configuration Jackson, sans troncature.
        String resultat = mvc.perform(post("/skillmatch/candidatures").header("Authorization", "Bearer test").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.statut").value("RECUE")).andExpect(jsonPath("$.dateCandidature").isNotEmpty()).andExpect(jsonPath("$.cvId").isEmpty())
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(resultat).get("id").asLong(); // Identifiant réellement généré.
        mvc.perform(get("/skillmatch/candidatures/{id}", id).header("Authorization", "Bearer test")).andExpect(status().isOk());
        mvc.perform(get("/skillmatch/candidatures").header("Authorization", "Bearer test")).andExpect(status().isOk());
        mvc.perform(put("/skillmatch/candidatures/{id}", id).header("Authorization", "Bearer test").contentType(MediaType.APPLICATION_JSON).content("{\"lettre\":\"Lettre de motivation modifiée pour ce poste\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.cvId").isEmpty());
        mvc.perform(post("/skillmatch/candidatures").header("Authorization", "Bearer test").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isConflict()); // Doublon.
        mvc.perform(delete("/skillmatch/candidatures/{id}", id).header("Authorization", "Bearer test")).andExpect(status().isNoContent());
        assertEquals(CandidatureStatut.RETIREE, candidatures.findById(id).orElseThrow().getStatut()); // Retrait logique.
        when(identites.courante("Bearer test")).thenReturn(new Identite(1L, "Admin", "admin@example.invalid", "ADMIN"));
        mvc.perform(delete("/skillmatch/candidatures/{id}", id).header("Authorization", "Bearer test")).andExpect(status().isNoContent()); // Suppression ADMIN.
        assertFalse(candidatures.existsById(id)); // L'offre reste conservée.
        assertTrue(offres.existsById(offre.getId()));
    }
}
