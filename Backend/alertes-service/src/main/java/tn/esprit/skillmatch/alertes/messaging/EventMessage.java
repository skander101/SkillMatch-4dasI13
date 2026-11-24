package tn.esprit.skillmatch.alertes.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.UUID;

/** Format commun des événements (dossier technique §11) : eventId, type, occurredAt, source, data. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EventMessage<T>(
        String eventId,
        String type,
        Instant occurredAt,
        String source,
        T data
) {
    public static <T> EventMessage<T> of(String type, String source, T data) {
        return new EventMessage<>(UUID.randomUUID().toString(), type, Instant.now(), source, data);
    }
}
