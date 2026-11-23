package tn.esprit.skillmatch.commun.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Corps de POST /skillmatch/annuaire/utilisateurs/batch.
 * Liste d'identifiants à résoudre en (id, nom, rôle).
 */
public record AnnuaireBatchRequest(
        @NotNull(message = "ids est obligatoire")
        List<Long> ids
) {
}
