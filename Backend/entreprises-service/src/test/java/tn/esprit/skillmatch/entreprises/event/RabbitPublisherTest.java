package tn.esprit.skillmatch.entreprises.event;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import tn.esprit.skillmatch.entreprises.entity.StatutModeration;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RabbitPublisherTest {

    @Test
    void publishSendsEventOnExpectedExchangeAndRoutingKey() throws AmqpException {
        RecordingRabbitTemplate rabbitTemplate = new RecordingRabbitTemplate();
        RabbitPublisher publisher = new RabbitPublisher(rabbitTemplate, "test.events");
        EvenementEntreprise event = EvenementEntreprise.avisModere(donnees(StatutModeration.APPROUVE, 4.5));

        publisher.publish(event);

        assertEquals("test.events", rabbitTemplate.exchange);
        assertEquals(EvenementEntreprise.AVIS_MODERE, rabbitTemplate.routingKey);
        assertEquals(event, rabbitTemplate.payload);
        assertEquals(event.eventId().toString(), rabbitTemplate.correlationData.getId());

        Message message = new Message(new byte[0], new MessageProperties());
        rabbitTemplate.postProcessor.postProcessMessage(message);
        assertEquals(event.eventId().toString(), message.getMessageProperties().getMessageId());
        assertEquals(EvenementEntreprise.AVIS_MODERE, message.getMessageProperties().getHeaders().get("type"));
        assertEquals(MessageDeliveryMode.PERSISTENT, message.getMessageProperties().getDeliveryMode());
    }

    @Test
    void publishDoesNotFailModerationWhenBrokerIsUnavailable() {
        RecordingRabbitTemplate rabbitTemplate = new RecordingRabbitTemplate();
        rabbitTemplate.failure = new AmqpException("broker indisponible");
        RabbitPublisher publisher = new RabbitPublisher(rabbitTemplate, "test.events");
        EvenementEntreprise event = EvenementEntreprise.avisModere(donnees(StatutModeration.REJETE, 0.0));

        assertDoesNotThrow(() -> publisher.publish(event));
        assertNotNull(event.eventId());
    }

    private EvenementEntreprise.AvisModere donnees(StatutModeration statut, double noteMoyenne) {
        return new EvenementEntreprise.AvisModere(
                2L,
                1L,
                10L,
                "Alice Martin",
                5,
                statut,
                noteMoyenne,
                LocalDateTime.of(2026, 11, 25, 22, 0)
        );
    }

    private static class RecordingRabbitTemplate extends RabbitTemplate {

        private String exchange;
        private String routingKey;
        private Object payload;
        private MessagePostProcessor postProcessor;
        private CorrelationData correlationData;
        private AmqpException failure;

        @Override
        public void convertAndSend(String exchange, String routingKey, Object object,
                                   MessagePostProcessor postProcessor, CorrelationData correlationData) {
            if (failure != null) {
                throw failure;
            }
            this.exchange = exchange;
            this.routingKey = routingKey;
            this.payload = object;
            this.postProcessor = postProcessor;
            this.correlationData = correlationData;
        }
    }
}
