package tn.esprit.skillmatch.entreprises.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class RabbitPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;

    public RabbitPublisher(RabbitTemplate rabbitTemplate,
                           @Value("${skillmatch.events.exchange:skillmatch.events}") String exchange) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(EvenementEntreprise event) {
        try {
            MessagePostProcessor postProcessor = message -> enrichir(message, event);
            rabbitTemplate.convertAndSend(exchange, event.type(), event, postProcessor,
                    new CorrelationData(event.eventId().toString()));
        } catch (AmqpException exception) {
            LOGGER.warn("RabbitMQ NON ENVOYE : {} ({})", event.type(), exception.getMessage());
        }
    }

    private Message enrichir(Message message, EvenementEntreprise event) {
        message.getMessageProperties().setMessageId(event.eventId().toString());
        message.getMessageProperties().setHeader("type", event.type());
        message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        return message;
    }
}
