package tn.esprit.skillmatch.profils.messaging;

import org.springframework.stereotype.Component;
import tn.esprit.skillmatch.profils.event.EventEnvelope;
import tn.esprit.skillmatch.profils.service.ProfilService;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TestPassedConsumer {

    private final ProfilService profilService;
    private final ProfileUpdatedPublisher publisher;
    private final Set<String> evenementsTraites = ConcurrentHashMap.newKeySet();

    public TestPassedConsumer(ProfilService profilService, ProfileUpdatedPublisher publisher) {
        this.profilService = profilService;
        this.publisher = publisher;
    }

    public void consommer(EventEnvelope<Map<String, Object>> envelope) {
        if (envelope == null || envelope.eventId() == null
                || !evenementsTraites.add(envelope.eventId())) {
            return;
        }
        Map<String, Object> raw = envelope.data();
        if (raw == null) {
            return;
        }
        Object userId = raw.get("userId");
        Object badge = raw.get("badge");
        if (userId instanceof Number number && badge instanceof String value && !value.isBlank()) {
            profilService.ajouterBadge(number.longValue(), value);
            publisher.publier(number.longValue());
        }
    }
}
