package tn.esprit.skillmatch.alertes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import tn.esprit.skillmatch.alertes.entity.ContratType;
import tn.esprit.skillmatch.alertes.entity.Frequence;

import java.util.List;

public record AlerteRequest(
        @NotEmpty @Size(max = 10) List<@NotBlank @Size(max = 50) String> motsCles,
        @Size(max = 100) String localisation,
        ContratType contrat,
        Frequence frequence
) {
}
