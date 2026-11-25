package tn.esprit.skillmatch.profils.messaging;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tn.esprit.skillmatch.profils.dto.CompletenessResponse;
import tn.esprit.skillmatch.profils.event.EventEnvelope;
import tn.esprit.skillmatch.profils.event.ProfileUpdatedData;
import tn.esprit.skillmatch.profils.service.ProfilService;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
public class ProfileUpdatedPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final ProfilService profilService;
    private final String exchange;
    private final String routingKey;

    public ProfileUpdatedPublisher(RabbitTemplate rabbitTemplate, ProfilService profilService,
                                   @Value("${messaging.exchange}") String exchange,
                                   @Value("${messaging.published-routing-key}") String routingKey) {
        this.rabbitTemplate = rabbitTemplate;
        this.profilService = profilService;
        this.exchange = exchange;
        this.routingKey = routingKey;
    }

    public void publier(Long userId) {
        CompletenessResponse completude = profilService.completude(userId);
        ProfileUpdatedData data = new ProfileUpdatedData(
                userId, completude.completeness(),
                Map.of("completude", completude.completeness())
        );
        EventEnvelope<ProfileUpdatedData> event = new EventEnvelope<>(
                UUID.randomUUID().toString(), "ProfileUpdated", Instant.now(), "profils-service", data
        );
        rabbitTemplate.convertAndSend(exchange, routingKey, event);
    }
}
