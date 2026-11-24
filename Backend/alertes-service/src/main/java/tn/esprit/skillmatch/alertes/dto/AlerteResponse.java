package tn.esprit.skillmatch.alertes.dto;

import tn.esprit.skillmatch.alertes.entity.ContratType;
import tn.esprit.skillmatch.alertes.entity.Frequence;

import java.time.LocalDateTime;
import java.util.List;

public record AlerteResponse(
        Long id,
        Long userId,
        List<String> motsCles,
        String localisation,
        ContratType contrat,
        Frequence frequence,
        boolean active,
        LocalDateTime dateCreation
) {
}
