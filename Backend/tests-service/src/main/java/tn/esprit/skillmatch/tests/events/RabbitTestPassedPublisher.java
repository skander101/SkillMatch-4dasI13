package tn.esprit.skillmatch.tests.events;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

class RabbitTestPassedPublisher {

    static final String ROUTING_KEY = "TestPassed";
    private static final String SOURCE = "tests-service";
    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitTestPassedPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final TopicExchange exchange;
    private final ObjectMapper objectMapper;

    RabbitTestPassedPublisher(RabbitTemplate rabbitTemplate, TopicExchange exchange, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.objectMapper = objectMapper;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(TestPassed event) {
        String eventId = ROUTING_KEY + ":" + event.resultatId();
        var envelope = new Envelope(eventId, ROUTING_KEY, event.date().toInstant(ZoneOffset.UTC), SOURCE, event);
        try {
            var message = MessageBuilder.withBody(objectMapper.writeValueAsBytes(envelope))
                    .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                    .setContentEncoding(StandardCharsets.UTF_8.name())
                    .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                    .setMessageId(eventId)
                    .setType(ROUTING_KEY)
                    .build();
            rabbitTemplate.send(exchange.getName(), ROUTING_KEY, message);
        } catch (AmqpException | JsonProcessingException error) {
            LOGGER.warn("Publication de l’événement {} échouée. {}", eventId, error.getMessage());
            LOGGER.debug("Détail de l’échec de publication", error);
        }
    }

    private record Envelope(String eventId, String type, Instant occurredAt, String source, TestPassed data) {
    }
}
