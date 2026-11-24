package tn.esprit.skillmatch.profils.dto;

import java.util.List;

public record ProfilResponse(
        Long id,
        Long userId,
        String titre,
        String localisation,
        List<String> competences,
        List<String> experiences,
        List<String> badges,
        Integer completude
) {
}
