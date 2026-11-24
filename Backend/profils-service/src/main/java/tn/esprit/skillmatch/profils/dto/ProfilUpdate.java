package tn.esprit.skillmatch.profils.dto;

import jakarta.validation.constraints.Size;

import java.util.List;

public record ProfilUpdate(
        @Size(max = 150) String titre,
        @Size(max = 150) String localisation,
        List<@Size(max = 100) String> competences,
        List<@Size(max = 250) String> experiences,
        List<@Size(max = 100) String> badges
) {
    public ProfilUpdate {
        titre = trim(titre);
        localisation = trim(localisation);
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
