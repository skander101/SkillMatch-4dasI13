
package tn.esprit.skillmatch.offres.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import java.util.ArrayList;
import java.util.List;


// ========== CONFIGURATION RABBITMQ ==========

// Indique que cette classe configure RabbitMQ dans Spring.
@Configuration

// Classe qui contient la configuration des échanges et des files RabbitMQ.
public class RabbitConfig {


    // ========== 1. CRÉATION DE L'EXCHANGE ==========

    // Déclare un objet géré automatiquement par Spring.
    @Bean

    // Configure l'exchange qui reçoit les événements.
    TopicExchange echangeEvenements(@Value("${skillmatch.events.exchange:skillmatch.events}") String nom) {

        // Crée un exchange de type Topic.
        return new TopicExchange(nom, true, false);
    }


    // ========== 2. CRÉATION DES FILES D'ÉVÉNEMENTS ==========

    // Déclare les files et leurs liaisons dans Spring.
    @Bean

    // Prépare les files RabbitMQ pour les différents événements.
    Declarables filesEvenements(TopicExchange exchange,
                                @Value("${skillmatch.events.queue-prefix:skillmatch.offres}") String prefixe) {

        // Crée une liste vide pour stocker les files et les liaisons.
        List<Declarable> elements = new ArrayList<>();

        // Parcourt les trois types d'événements du microservice.
        for (String type : List.of("OfferPublished", "ApplicationSubmitted", "StatusChanged")) {

            // Crée une file durable pour chaque type d'événement.
            Queue file = new Queue(prefixe + "." + type, true);

            // Ajoute la file dans la liste.
            elements.add(file);

            // Relie la file à l'exchange selon le type d'événement.
            elements.add(BindingBuilder.bind(file).to(exchange).with(type));
        }

        // Retourne toutes les files et leurs liaisons à Spring.
        return new Declarables(elements);
    }


    // ========== 3. CONVERSION DES MESSAGES EN JSON ==========

    // Déclare le convertisseur JSON dans Spring.
    @Bean

    // Configure la conversion des messages RabbitMQ en JSON.
    Jackson2JsonMessageConverter conversionJson(ObjectMapper json) {

        // Crée un convertisseur JSON utilisant la configuration Jackson.
        return new Jackson2JsonMessageConverter(json);
    }
}
