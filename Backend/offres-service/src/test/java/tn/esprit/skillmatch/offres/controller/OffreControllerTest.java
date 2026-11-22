package tn.esprit.skillmatch.offres.controller; // Tests HTTP sans base ni serveur externe.

import com.fasterxml.jackson.databind.ObjectMapper; // Écriture JSON des formulaires.
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule; // Support des dates Java.
import org.junit.jupiter.api.BeforeEach; // Préparation commune.
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test; // Tests JUnit.
import org.junit.jupiter.api.extension.ExtendWith; // Active Mockito.
import org.mockito.Mock; // Repositories et identité simulés.
import org.mockito.junit.jupiter.MockitoExtension; // Support Mockito.
import org.springframework.dao.DataIntegrityViolationException; // Conflit SQL simulé.
import org.springframework.http.HttpStatus; // Erreurs d'authentification.
import org.springframework.http.MediaType; // Type application/json.
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter; // Convertisseur HTTP JSON.
import org.springframework.test.web.servlet.MockMvc; // Appelle les routes sans ouvrir un port.
import org.springframework.test.web.servlet.setup.MockMvcBuilders; // Construit l'environnement HTTP de test.
import org.springframework.web.server.ResponseStatusException; // Erreur renvoyée par l'identité.
import tn.esprit.skillmatch.offres.dto.OffreRequest; // Données de formulaire.
import tn.esprit.skillmatch.offres.entity.Offre; // Offre de test.
import tn.esprit.skillmatch.offres.enums.*; // Contrat et statut.
import tn.esprit.skillmatch.offres.exception.GlobalExceptionHandler; // Format des erreurs.
import tn.esprit.skillmatch.offres.repository.*; // Faux accès à la base.
import tn.esprit.skillmatch.offres.security.IdentiteService; // Fausse authentification.
import tn.esprit.skillmatch.offres.security.IdentiteService.Identite; // Utilisateur vérifié simulé.
import tn.esprit.skillmatch.offres.service.OffreService; // Vrai service utilisé par les routes.
import java.time.LocalDate; // Date valide.
import java.util.List; // Compétences.
import java.util.Optional; // Offre trouvée.
import static org.mockito.Mockito.*; // Comportements des mocks.
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*; // GET, POST, PUT et DELETE.
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*; // Statuts et JSON attendus.

@ExtendWith(MockitoExtension.class) // Aucun appel réseau.
class OffreControllerTest { // Teste contrôleur, validations et vrai service ensemble.
    @Mock OffreRepository offres; // Faux stockage.
    @Mock CandidatureRepository candidatures; // Faux contrôle de présence.
    @Mock IdentiteService identites; // Fausse identité côté serveur.
    private MockMvc mvc; // Client HTTP de test.
    private final ObjectMapper json = new ObjectMapper().registerModule(new JavaTimeModule()); // Conversion des dates.

    @BeforeEach // Avant chaque test.
    void preparer() { // Construit le contrôleur et ses règles réelles.
        OffreService service = new OffreService(offres, candidatures, identites, mock(org.springframework.context.ApplicationEventPublisher.class));
        mvc = MockMvcBuilders.standaloneSetup(new OffreController(service)) // Routes du contrôleur.
                .setControllerAdvice(new GlobalExceptionHandler()) // Gestion des erreurs réelle.
                .setMessageConverters(new MappingJackson2HttpMessageConverter(json)).build(); // Support JSON.
    } // Fin de la préparation.

    @Test // Création HTTP.
    void creerRetourne201EtLeProprietaireDeSession() throws Exception { // Vérifie la réponse REST.
        connecter("RECRUTEUR"); // Recruteur 1.
        when(offres.save(any())).thenAnswer(appel -> { // Simule l'identifiant généré.
            Offre offre = appel.getArgument(0); // Offre reçue par le repository.
            offre.setId(10L); // Identifiant généré simulé.
            return offre; // Retourne l'offre persistée.
        }); // Fin de la simulation.
        mvc.perform(post("/skillmatch/offres").header("Authorization", "Bearer test") // Route de création.
                .contentType(MediaType.APPLICATION_JSON).content(formulaire())) // Formulaire valide.
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(10)) // Code 201 et identifiant.
                .andExpect(jsonPath("$.recruteurId").value(1)).andExpect(jsonPath("$.statut").value("BROUILLON")); // Propriétaire et statut fiables.
    } // Fin du test.

    @Test // Liste et route spéciale.
    void listerEtMesOffresRetournent200() throws Exception { // Vérifie aussi que mes-offres n'est pas traité comme un id.
        connecter("RECRUTEUR"); // Gestionnaire connecté.
        mvc.perform(get("/skillmatch/offres").header("Authorization", "Bearer test")).andExpect(status().isOk()); // Liste.
        mvc.perform(get("/skillmatch/offres/mes-offres").header("Authorization", "Bearer test")).andExpect(status().isOk()); // Liste personnelle.
    } // Fin du test.

    @Test // Authentification requise.
    void jwtAbsentRetourne401() throws Exception { // Le service refuse une demande anonyme.
        when(identites.courante(null)).thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentification requise")); // Réponse de l'identité.
        mvc.perform(get("/skillmatch/offres")).andExpect(status().isUnauthorized()) // Code 401.
                .andExpect(jsonPath("$.message").value("Authentification requise")); // Message exploitable par Angular.
        verifyNoInteractions(offres); // La base n'est pas consultée.
    } // Fin du test.

    @Test // Lecture seule candidat.
    void creationCandidatRetourne403() throws Exception { // Même avec un formulaire valide.
        connecter("CANDIDAT"); // Rôle insuffisant.
        mvc.perform(post("/skillmatch/offres").header("Authorization", "Bearer test") // Demande authentifiée.
                .contentType(MediaType.APPLICATION_JSON).content(formulaire())).andExpect(status().isForbidden()); // Code 403.
    } // Fin du test.

    @Test // Identifiant absent.
    void offreInconnueRetourne404() throws Exception { // Pas d'offre dans le repository.
        connecter("ADMIN"); // Rôle autorisé.
        mvc.perform(get("/skillmatch/offres/999").header("Authorization", "Bearer test")) // Offre inconnue.
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404)); // Format d'erreur.
    } // Fin du test.

    @Test // Validation du DTO.
    void titreVideEtSalaireNegatifRetournent400() throws Exception { // Vérifie les contraintes @Valid.
        String invalide = formulaire().replace("\"Java\"", "\"\"").replace("2000.0", "-1.0"); // Champs volontairement invalides.
        mvc.perform(post("/skillmatch/offres").contentType(MediaType.APPLICATION_JSON).content(invalide)) // Pas d'appel métier si le formulaire est invalide.
                .andExpect(status().isBadRequest()); // Code 400.
        verifyNoInteractions(identites, offres); // Validation avant le service.
    } // Fin du test.

    @Test // Erreurs de format.
    void enumInconnuJsonMalFormeEtIdTexteRetournent400() throws Exception { // Vérifie le parseur HTTP.
        mvc.perform(post("/skillmatch/offres").contentType(MediaType.APPLICATION_JSON).content(formulaire().replace("CDI", "INCONNU"))) // Contrat non défini.
                .andExpect(status().isBadRequest()); // Code 400.
        mvc.perform(post("/skillmatch/offres").contentType(MediaType.APPLICATION_JSON).content("{")) // JSON incomplet.
                .andExpect(status().isBadRequest()); // Code 400.
        mvc.perform(get("/skillmatch/offres/abc")).andExpect(status().isBadRequest()); // Identifiant non numérique.
    } // Fin du test.

    @Test // Suppression simple.
    void supprimerRetourne204() throws Exception { // Sans candidature associée.
        connecter("RECRUTEUR"); // Propriétaire 1.
        existante(); // Offre appartenant à 1.
        mvc.perform(delete("/skillmatch/offres/10").header("Authorization", "Bearer test")) // Supprime l'offre.
                .andExpect(status().isNoContent()).andExpect(content().string("")); // Aucun corps après succès.
    } // Fin du test.

    @Test // Protection métier.
    void suppressionAvecCandidatureRetourne409() throws Exception { // Vérifie le code de conflit.
        connecter("ADMIN"); // Administrateur autorisé.
        existante(); // Offre à supprimer.
        when(candidatures.existsByOffreId(10L)).thenReturn(true); // Candidature existante.
        mvc.perform(delete("/skillmatch/offres/10").header("Authorization", "Bearer test")).andExpect(status().isConflict()); // Code 409.
    } // Fin du test.

    @Test // Conflit SQL tardif.
    void conflitDeCleEtrangereRetourneAussi409() throws Exception { // Couvre une insertion simultanée de candidature.
        connecter("ADMIN"); // Administrateur.
        existante(); // Offre existante.
        doThrow(new DataIntegrityViolationException("Erreur SQL interne")).when(offres).flush(); // La base bloque la suppression.
        mvc.perform(delete("/skillmatch/offres/10").header("Authorization", "Bearer test")) // Déclenche le conflit.
                .andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409)); // Pas de trace SQL exposée.
    } // Fin du test.

    @Test // Modification REST.
    void modifierRetourne200() throws Exception { // Vérifie PUT.
        connecter("RECRUTEUR"); // Propriétaire.
        Offre offre = existante(); // Offre modifiable.
        when(offres.save(offre)).thenReturn(offre); // Simule la sauvegarde.
        mvc.perform(put("/skillmatch/offres/10").header("Authorization", "Bearer test") // Route PUT.
                .contentType(MediaType.APPLICATION_JSON).content(formulaire().replace("BROUILLON", "PUBLIEE"))) // Publication.
                .andExpect(status().isOk()).andExpect(jsonPath("$.statut").value("PUBLIEE")); // Publication confirmée.
    } // Fin du test.

    private void connecter(String role) { // Identité locale au test.
        when(identites.courante("Bearer test")).thenReturn(new Identite(1L, "Test", "test@example.invalid", role)); // Le rôle vient du faux serveur.
    } // Fin du helper.

    private Offre existante() { // Offre de base des scénarios.
        Offre offre = new Offre(); // Crée l'objet.
        offre.setId(10L); // Identifiant connu.
        offre.setRecruteurId(1L); // Propriétaire.
        when(offres.findById(10L)).thenReturn(Optional.of(offre)); // Simule la lecture.
        return offre; // Retourne l'objet.
    } // Fin du helper.

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT"})
    void ajoutEtModificationRefusentLesChampsVidesEtLesLongueursInvalides(String methode) throws Exception {
        var base = json.readTree(formulaire());
        for (String champ : List.of("titre", "description", "localisation", "salaire", "competences", "contrat", "dateExpiration", "statut", "nomEntreprise")) {
            var corps = base.deepCopy(); ((com.fasterxml.jackson.databind.node.ObjectNode) corps).putNull(champ);
            var route = methode.equals("POST") ? post("/skillmatch/offres") : put("/skillmatch/offres/10");
            mvc.perform(route.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(corps))).andExpect(status().isBadRequest());
        }
        for (String champ : List.of("titre", "description", "nomEntreprise")) {
            int minimum = champ.equals("titre") ? 3 : champ.equals("nomEntreprise") ? 2 : 20;
            int maximum = champ.equals("description") ? 10000 : 255;
            for (String texte : List.of(" ", "a".repeat(minimum - 1), "a".repeat(maximum + 1))) {
                var corps = base.deepCopy(); ((com.fasterxml.jackson.databind.node.ObjectNode) corps).put(champ, texte);
                var route = methode.equals("POST") ? post("/skillmatch/offres") : put("/skillmatch/offres/10");
                mvc.perform(route.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(corps))).andExpect(status().isBadRequest());
            }
        }
        verifyNoInteractions(identites, offres);
    }

    @Test
    void creationSansTestRequisEstAutorisee() throws Exception {
        connecter("RECRUTEUR");
        when(offres.save(any())).thenAnswer(appel -> appel.getArgument(0));
        var corps = (com.fasterxml.jackson.databind.node.ObjectNode) json.readTree(formulaire());
        corps.putNull("testRequisId");
        corps.putNull("entrepriseId");
        mvc.perform(post("/skillmatch/offres").header("Authorization", "Bearer test")
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(corps)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.testRequisId").isEmpty()).andExpect(jsonPath("$.nomEntreprise").value("Entreprise de test"));
    }

    private String formulaire() throws Exception { // Formulaire valide commun.
        return json.writeValueAsString(new OffreRequest("Java", "Description détaillée du poste Java", List.of("Java"), "Tunis", ContratType.CDI, 2000.0, LocalDate.now().plusDays(5), OffreStatut.BROUILLON, 1L, 1L, null, "Entreprise de test")); // Génère un vrai JSON.
    } // Fin du formulaire.
} // Fin des tests HTTP.
