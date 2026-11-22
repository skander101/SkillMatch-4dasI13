package tn.esprit.skillmatch.offres.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class RabbitConfig {
    @Bean
    TopicExchange echangeEvenements(@Value("${skillmatch.events.exchange:skillmatch.events}") String nom) {
        return new TopicExchange(nom, true, false);
    }

    @Bean
    Declarables filesEvenements(TopicExchange exchange,
                              @Value("${skillmatch.events.queue-prefix:skillmatch.offres}") String prefixe) {
        List<Declarable> elements = new ArrayList<>();
        for (String type : List.of("OfferPublished", "ApplicationSubmitted", "StatusChanged")) {
            Queue file = new Queue(prefixe + "." + type, true); // File durable, visible même sans consommateur.
            elements.add(file);
            elements.add(BindingBuilder.bind(file).to(exchange).with(type));
        }
        return new Declarables(elements);
    }

    @Bean
    Jackson2JsonMessageConverter conversionJson(ObjectMapper json) {
        return new Jackson2JsonMessageConverter(json); // Le même format de dates que l'API.
    }
}
