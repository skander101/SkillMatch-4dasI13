package tn.esprit.skillmatch.entreprises.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.skillmatch.entreprises.dto.EntrepriseRequest;
import tn.esprit.skillmatch.entreprises.dto.EntrepriseResponse;
import tn.esprit.skillmatch.entreprises.entity.Entreprise;
import tn.esprit.skillmatch.entreprises.repository.EntrepriseRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EntrepriseServiceTest {

    @Mock
    private EntrepriseRepository entrepriseRepository;

    @InjectMocks
    private EntrepriseService entrepriseService;

    @Test
    void createReturnsSavedEntreprise() {
        when(entrepriseRepository.save(any(Entreprise.class))).thenAnswer(invocation -> {
            Entreprise entreprise = invocation.getArgument(0);
            entreprise.setId(1L);
            return entreprise;
        });

        EntrepriseResponse response = entrepriseService.create(
                new EntrepriseRequest("TechNova", "Informatique", "Tunis", "Recrutement tech")
        );

        assertEquals(1L, response.id());
        assertEquals("TechNova", response.nom());
        assertEquals(0.0, response.noteMoyenne());
    }

    @Test
    void updateRejectsUnknownId() {
        when(entrepriseRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                entrepriseService.update(99L, new EntrepriseRequest("Nouveau", "IT", "Sfax", null))
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void deleteRejectsUnknownId() {
        when(entrepriseRepository.existsById(99L)).thenReturn(false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                entrepriseService.delete(99L)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(entrepriseRepository).existsById(99L);
    }
}
