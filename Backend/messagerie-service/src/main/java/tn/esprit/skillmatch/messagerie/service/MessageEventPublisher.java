package tn.esprit.skillmatch.messagerie.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import tn.esprit.skillmatch.messagerie.config.RabbitMQConfig;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Publie un événement MessageSent sur RabbitMQ pour chaque destinataire.
 * L'envoi est asynchrone (@Async) : un échec RabbitMQ ne bloque jamais l'envoi du message.
 */
@Service
public class MessageEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(MessageEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public MessageEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Publie un événement par destinataire (tous les participants sauf l'expéditeur).
     */
    @Async
    public void publish(String messageId, String conversationId, Long senderId,
                        List<Long> participantIds, String contenu, LocalDateTime date) {
        for (Long participantId : participantIds) {
            if (participantId.equals(senderId)) {
                continue; // ne pas notifier l'expéditeur
            }
            try {
                Map<String, Object> event = new HashMap<>();
                event.put("messageId", messageId);
                event.put("conversationId", conversationId);
                event.put("senderId", senderId);
                event.put("recipientId", participantId);
                event.put("contenu", contenu);
                event.put("date", date.toString());

                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.EXCHANGE,
                        RabbitMQConfig.ROUTING_KEY,
                        event
                );
                log.info("Événement MessageSent publié pour le destinataire {}", participantId);
            } catch (Exception e) {
                log.error("Échec de publication de l'événement MessageSent pour le destinataire {} : {}",
                        participantId, e.getMessage(), e);
                // Ne jamais faire échouer l'envoi du message à cause de RabbitMQ
            }
        }
    }
}
