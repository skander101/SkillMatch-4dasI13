package tn.esprit.skillmatch.entreprises.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Bean
    public TopicExchange skillmatchEventsExchange(
            @Value("${skillmatch.events.exchange:skillmatch.events}") String exchangeName) {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    public Declarables entrepriseEventsDeclarables(
            TopicExchange skillmatchEventsExchange,
            @Value("${skillmatch.events.queue-prefix:skillmatch.entreprises}") String queuePrefix) {
        Queue avisModereQueue = new Queue(queuePrefix + "." + EvenementEntreprise.AVIS_MODERE, true);
        Queue profileUpdatedQueue = new Queue(queuePrefix + "." + EvenementEntreprise.PROFILE_UPDATED, true);

        Binding avisModereBinding = BindingBuilder.bind(avisModereQueue)
                .to(skillmatchEventsExchange)
                .with(EvenementEntreprise.AVIS_MODERE);
        Binding profileUpdatedBinding = BindingBuilder.bind(profileUpdatedQueue)
                .to(skillmatchEventsExchange)
                .with(EvenementEntreprise.PROFILE_UPDATED);

        return new Declarables(avisModereQueue, profileUpdatedQueue, avisModereBinding, profileUpdatedBinding);
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
