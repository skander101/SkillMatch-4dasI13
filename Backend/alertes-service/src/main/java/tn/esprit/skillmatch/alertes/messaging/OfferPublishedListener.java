package tn.esprit.skillmatch.alertes.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tn.esprit.skillmatch.alertes.entity.AlerteEmploi;
import tn.esprit.skillmatch.alertes.service.AlerteService;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Scénario « nouvelle offre » : offres-service émet OfferPublished -> alertes-service évalue les critères
 * -> publie AlertMatched pour chaque alerte active correspondante -> commun-service notifie le candidat.
 */
@Component
@ConditionalOnProperty(name = "alertes.messaging.enabled", havingValue = "true")
public class OfferPublishedListener {

    private static final Logger log = LoggerFactory.getLogger(OfferPublishedListener.class);

    private final AlerteService alerteService;
    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String routingKeyAlertMatched;
    /** eventId déjà traités : un message reçu deux fois n'envoie pas deux notifications. */
    private final Set<String> processedEvents = ConcurrentHashMap.newKeySet();

    public OfferPublishedListener(AlerteService alerteService, RabbitTemplate rabbitTemplate,
                                  @Value("${alertes.messaging.exchange}") String exchange,
                                  @Value("${alertes.messaging.routing-key-alert-matched}") String routingKeyAlertMatched) {
        this.alerteService = alerteService;
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.routingKeyAlertMatched = routingKeyAlertMatched;
    }

    @RabbitListener(queues = "${alertes.messaging.queue-offer-published}")
    public void onOfferPublished(EventMessage<OfferPublishedData> event) {
        if (event == null || event.data() == null) {
            log.warn("OfferPublished ignoré : message vide");
            return;
        }
        if (event.eventId() != null && !processedEvents.add(event.eventId())) {
            log.info("OfferPublished {} déjà traité", event.eventId());
            return;
        }
        OfferPublishedData offre = event.data();
        List<AlerteEmploi> alertes = alerteService.findCorrespondantes(
                offre.titre(), offre.competences(), offre.localisation(), offre.contrat());

        for (AlerteEmploi alerte : alertes) {
            AlertMatchedData data = new AlertMatchedData(alerte.getId(), alerte.getUserId(), offre.offerId(), offre.titre());
            rabbitTemplate.convertAndSend(exchange, routingKeyAlertMatched,
                    EventMessage.of("AlertMatched", "alertes-service", data));
        }
        log.info("OfferPublished offre={} : {} AlertMatched publié(s)", offre.offerId(), alertes.size());
    }
}
