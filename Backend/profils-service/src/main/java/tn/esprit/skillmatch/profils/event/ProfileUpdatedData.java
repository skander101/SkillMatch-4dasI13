package tn.esprit.skillmatch.profils.event;

import java.util.Map;

public record ProfileUpdatedData(
        Long userId,
        Integer completude,
        Map<String, Object> competences
) {
}
