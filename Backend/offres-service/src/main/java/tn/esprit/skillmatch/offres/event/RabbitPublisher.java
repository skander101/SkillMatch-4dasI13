package tn.esprit.skillmatch.offres.event;

import org.slf4j.*;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.*;
import java.util.concurrent.TimeUnit;

@Component
public class RabbitPublisher {
    private static final Logger log = LoggerFactory.getLogger(RabbitPublisher.class);
    private final RabbitTemplate rabbit;
    private final String exchange;

    public RabbitPublisher(RabbitTemplate rabbit, TopicExchange exchange) {
        this.rabbit = rabbit;
        this.exchange = exchange.getName();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void envoyer(EvenementOffres evenement) { // Une transaction annulée ne publie rien.
        try {
            CorrelationData suivi = new CorrelationData(evenement.eventId().toString());
            rabbit.convertAndSend(exchange, evenement.type(), evenement, message -> {
                message.getMessageProperties().setMessageId(evenement.eventId().toString());
                message.getMessageProperties().setType(evenement.type());
                message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                return message;
            }, suivi);
            var confirmation = suivi.getFuture().get(5, TimeUnit.SECONDS);
            if (!confirmation.isAck() || suivi.getReturned() != null) {
                throw new IllegalStateException("Message refusé ou sans file de destination");
            }
            log.info("RabbitMQ confirmé : {} eventId={}", evenement.type(), evenement.eventId());
        } catch (InterruptedException erreur) {
            Thread.currentThread().interrupt();
            log.error("RabbitMQ interrompu : {} eventId={}", evenement.type(), evenement.eventId(), erreur);
        } catch (Exception erreur) {
            // MySQL est déjà validé : ne pas faire croire au client que son CRUD a échoué.
            log.error("RabbitMQ NON ENVOYE : {} eventId={} ; opération MySQL conservée", evenement.type(), evenement.eventId(), erreur);
        }
    }
}
