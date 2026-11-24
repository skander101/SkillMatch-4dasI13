package tn.esprit.skillmatch.profils.dto;

import java.time.LocalDateTime;

public record CVResponse(
        Long id,
        Long profilId,
        String fichierUri,
        LocalDateTime dateUpload
) {
}
