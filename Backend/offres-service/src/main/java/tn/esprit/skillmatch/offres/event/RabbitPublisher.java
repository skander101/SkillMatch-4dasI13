
package tn.esprit.skillmatch.offres.event;

import org.slf4j.*;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.*;
import java.util.concurrent.TimeUnit;


// ========== CLASSE RABBIT PUBLISHER ==========

// Déclare cette classe comme un composant géré par Spring.
@Component

// Classe responsable de l'envoi des événements vers RabbitMQ.
public class RabbitPublisher {


    // ========== DÉCLARATION DES ATTRIBUTS ==========

    // Crée un outil pour afficher les messages dans la console.
    private static final Logger log = LoggerFactory.getLogger(RabbitPublisher.class);

    // Déclare l'outil utilisé pour envoyer les messages RabbitMQ.
    private final RabbitTemplate rabbit;

    // Stocke le nom de l'exchange RabbitMQ.
    private final String exchange;


    // ========== CONSTRUCTEUR ==========

    // Injecte les outils nécessaires pour communiquer avec RabbitMQ.
    public RabbitPublisher(RabbitTemplate rabbit, TopicExchange exchange) {

        // Initialise l'outil d'envoi des messages.
        this.rabbit = rabbit;

        // Récupère et enregistre le nom de l'exchange.
        this.exchange = exchange.getName();
    }


    // ========== ENVOYER UN ÉVÉNEMENT ==========

    // Attend que la transaction MySQL soit validée avant l'envoi.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)

    // Méthode qui envoie un événement vers RabbitMQ.
    public void envoyer(EvenementOffres evenement) {

        // Essaie d'envoyer le message.
        try {

            // Crée un identifiant pour suivre la confirmation du message.
            CorrelationData suivi = new CorrelationData(evenement.eventId().toString());

            // Envoie l'événement vers RabbitMQ.
            rabbit.convertAndSend(exchange, evenement.type(), evenement, message -> {

                // Ajoute l'identifiant unique au message.
                message.getMessageProperties().setMessageId(evenement.eventId().toString());

                // Ajoute le type d'événement au message.
                message.getMessageProperties().setType(evenement.type());

                // Rend le message persistant dans RabbitMQ.
                message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);

                // Retourne le message préparé pour l'envoi.
                return message;

            }, suivi);

            // Attend au maximum 5 secondes la confirmation de RabbitMQ.
            var confirmation = suivi.getFuture().get(5, TimeUnit.SECONDS);

            // Vérifie si RabbitMQ a refusé ou retourné le message.
            if (!confirmation.isAck() || suivi.getReturned() != null) {

                // Déclenche une erreur si l'envoi n'est pas confirmé correctement.
                throw new IllegalStateException("Message refusé ou sans file de destination");
            }

            // Affiche un message de succès dans les logs.
            log.info("RabbitMQ confirmé : {} eventId={}", evenement.type(), evenement.eventId());

            // Récupère une interruption pendant l'attente.
        } catch (InterruptedException erreur) {

            // Conserve l'information que le thread a été interrompu.
            Thread.currentThread().interrupt();

            // Affiche l'erreur d'interruption dans les logs.
            log.error("RabbitMQ interrompu : {} eventId={}", evenement.type(), evenement.eventId(), erreur);

            // Récupère les autres erreurs d'envoi.
        } catch (Exception erreur) {

            // Affiche l'échec de RabbitMQ dans les logs.
            // L'opération MySQL reste enregistrée.
            log.error("RabbitMQ NON ENVOYE : {} eventId={} ; opération MySQL conservée", evenement.type(), evenement.eventId(), erreur);
        }
    }
}
