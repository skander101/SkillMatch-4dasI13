package tn.esprit.skillmatch.alertes.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.skillmatch.alertes.dto.AlerteRequest;
import tn.esprit.skillmatch.alertes.dto.AlerteResponse;
import tn.esprit.skillmatch.alertes.entity.AlerteEmploi;
import tn.esprit.skillmatch.alertes.entity.ContratType;
import tn.esprit.skillmatch.alertes.entity.Frequence;
import tn.esprit.skillmatch.alertes.repository.AlerteEmploiRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Règles métier testées sans base de données (repository simulé avec Mockito). */
class AlerteServiceTest {

    private AlerteEmploiRepository repository;
    private AlerteService service;

    @BeforeEach
    void setUp() {
        repository = mock(AlerteEmploiRepository.class);
        service = new AlerteService(repository, 5);
        when(repository.save(any(AlerteEmploi.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void creeUneAlerteAvecDesMotsClesNormalises() {
        when(repository.findByUserId(1L)).thenReturn(List.of());
        when(repository.countByUserIdAndActiveTrue(1L)).thenReturn(0L);

        AlerteResponse response = service.create(1L,
                new AlerteRequest(List.of(" Java ", "java", "Spring Boot"), "Tunis", ContratType.CDI, null));

        assertEquals(List.of("java", "spring boot"), response.motsCles());
        assertEquals(Frequence.IMMEDIATE, response.frequence());
    }

    @Test
    void refuseUneSixiemeAlerteActive() {
        when(repository.findByUserId(1L)).thenReturn(List.of());
        when(repository.countByUserIdAndActiveTrue(1L)).thenReturn(5L);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                service.create(1L, new AlerteRequest(List.of("react"), null, null, null)));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatusCode());
        verify(repository, never()).save(any());
    }

    @Test
    void refuseUneAlerteIdentique() {
        AlerteEmploi existante = new AlerteEmploi(1L, List.of("java"), "Tunis", ContratType.CDI, Frequence.IMMEDIATE);
        existante.setId(7L);
        when(repository.findByUserId(1L)).thenReturn(List.of(existante));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                service.create(1L, new AlerteRequest(List.of("JAVA"), "tunis", ContratType.CDI, null)));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    void interditDeLireLAlerteDUnAutreCandidat() {
        AlerteEmploi alerte = new AlerteEmploi(2L, List.of("python"), null, null, null);
        alerte.setId(3L);
        when(repository.findById(3L)).thenReturn(Optional.of(alerte));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                service.findById(1L, 3L));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    void renvoie404SiLAlerteNExistePas() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                service.delete(1L, 99L));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void refuseDeReactiverAuDelaDuQuota() {
        AlerteEmploi alerte = new AlerteEmploi(1L, List.of("java"), null, null, null);
        alerte.setId(4L);
        alerte.setActive(false);
        when(repository.findById(4L)).thenReturn(Optional.of(alerte));
        when(repository.countByUserIdAndActiveTrue(1L)).thenReturn(5L);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                service.changeActive(1L, 4L, true));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatusCode());
    }
}
