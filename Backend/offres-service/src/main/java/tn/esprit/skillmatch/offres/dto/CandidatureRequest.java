package tn.esprit.skillmatch.offres.dto; // Formulaire de création.

import jakarta.validation.constraints.*; // Validation des champs.

public record CandidatureRequest( // Seules les informations nécessaires pour postuler.
        @NotNull @Positive Long offreId, // Offre choisie.
        @NotBlank @Size(min = 20, max = 10000) String lettre, // Lettre obligatoire.
        @NotNull @Positive Long cvId, // Référence CV, sans appel Profils pour cette phase.
        @Positive Long userId // Réservé à ADMIN ; le candidat utilise sa session.
) {
    public CandidatureRequest { lettre = lettre == null ? null : lettre.trim(); }
}
