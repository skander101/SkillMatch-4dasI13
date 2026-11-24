package tn.esprit.skillmatch.alertes.dto;

import java.time.LocalDateTime;

public record FavoriResponse(
        Long id,
        Long userId,
        Long offerId,
        LocalDateTime date
) {
}
