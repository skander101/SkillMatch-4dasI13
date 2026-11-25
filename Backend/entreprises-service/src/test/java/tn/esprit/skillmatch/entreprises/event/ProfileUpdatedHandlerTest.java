package tn.esprit.skillmatch.entreprises.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.skillmatch.entreprises.entity.AvisEntreprise;
import tn.esprit.skillmatch.entreprises.repository.AvisEntrepriseRepository;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileUpdatedHandlerTest {

    @Mock
    private AvisEntrepriseRepository avisEntrepriseRepository;

    private ProfileUpdatedHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ProfileUpdatedHandler(avisEntrepriseRepository, new ObjectMapper());
    }

    @Test
    void refreshesStoredNameForAllAvisOfUser() {
        AvisEntreprise premierAvis = avis("Ancien Nom");
        AvisEntreprise secondAvis = avis("Ancien Nom");
        when(avisEntrepriseRepository.findByUtilisateurId(10L)).thenReturn(List.of(premierAvis, secondAvis));

        handler.handle(event(Map.of("userId", 10L, "nom", "  Nouveau Nom  ")));

        assertEquals("Nouveau Nom", premierAvis.getNomUtilisateur());
        assertEquals("Nouveau Nom", secondAvis.getNomUtilisateur());
        verify(avisEntrepriseRepository).saveAll(List.of(premierAvis, secondAvis));
    }

    @Test
    void ignoresBlankName() {
        handler.handle(event(Map.of("userId", 10L, "nom", "   ")));

        verify(avisEntrepriseRepository, never()).findByUtilisateurId(10L);
        verify(avisEntrepriseRepository, never()).saveAll(anyList());
    }

    @Test
    void ignoresUnknownUser() {
        when(avisEntrepriseRepository.findByUtilisateurId(99L)).thenReturn(List.of());

        handler.handle(event(Map.of("userId", 99L, "nom", "Nouveau Nom")));

        verify(avisEntrepriseRepository, never()).saveAll(anyList());
    }

    @Test
    void ignoresOtherEventTypes() {
        EvenementEntreprise event = new EvenementEntreprise(
                UUID.randomUUID(),
                EvenementEntreprise.AVIS_MODERE,
                Instant.now(),
                Map.of("userId", 10L, "nom", "Nouveau Nom")
        );

        handler.handle(event);

        verify(avisEntrepriseRepository, never()).findByUtilisateurId(10L);
    }

    private AvisEntreprise avis(String nomUtilisateur) {
        AvisEntreprise avis = new AvisEntreprise();
        avis.setUtilisateurId(10L);
        avis.setNomUtilisateur(nomUtilisateur);
        return avis;
    }

    private EvenementEntreprise event(Map<String, Object> donnees) {
        return new EvenementEntreprise(
                UUID.randomUUID(),
                EvenementEntreprise.PROFILE_UPDATED,
                Instant.now(),
                donnees
        );
    }
}
