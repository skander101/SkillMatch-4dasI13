package tn.esprit.skillmatch.profils.event;

import java.time.Instant;
import java.util.Map;

public record EventEnvelope<T>(
        String eventId,
        String type,
        Instant occurredAt,
        String source,
        T data
) {
}
