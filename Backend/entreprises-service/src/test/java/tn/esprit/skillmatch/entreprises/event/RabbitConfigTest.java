package tn.esprit.skillmatch.entreprises.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RabbitConfigTest {

    private final RabbitConfig rabbitConfig = new RabbitConfig();

    @Test
    void declaresDurableTopicExchange() {
        TopicExchange exchange = rabbitConfig.skillmatchEventsExchange("test.events");

        assertEquals("test.events", exchange.getName());
        assertTrue(exchange.isDurable());
        assertFalse(exchange.isAutoDelete());
    }

    @Test
    void declaresQueuesAndBindingsForBusinessEvents() {
        TopicExchange exchange = rabbitConfig.skillmatchEventsExchange("test.events");
        Declarables declarables = rabbitConfig.entrepriseEventsDeclarables(exchange, "test.entreprises");

        Queue avisModereQueue = queue(declarables, "test.entreprises.AvisModere");
        Queue profileUpdatedQueue = queue(declarables, "test.entreprises.ProfileUpdated");
        assertTrue(avisModereQueue.isDurable());
        assertTrue(profileUpdatedQueue.isDurable());

        Binding avisModereBinding = binding(declarables, EvenementEntreprise.AVIS_MODERE);
        Binding profileUpdatedBinding = binding(declarables, EvenementEntreprise.PROFILE_UPDATED);
        assertEquals("test.entreprises.AvisModere", avisModereBinding.getDestination());
        assertEquals("test.entreprises.ProfileUpdated", profileUpdatedBinding.getDestination());
        assertEquals("test.events", avisModereBinding.getExchange());
        assertEquals("test.events", profileUpdatedBinding.getExchange());
    }

    @Test
    void providesJacksonMessageConverter() {
        assertNotNull(rabbitConfig.jackson2JsonMessageConverter(new ObjectMapper()));
    }

    private Queue queue(Declarables declarables, String name) {
        return declarables.getDeclarables().stream()
                .filter(Declarable.class::isInstance)
                .map(Declarable.class::cast)
                .filter(Queue.class::isInstance)
                .map(Queue.class::cast)
                .filter(queue -> queue.getName().equals(name))
                .findFirst()
                .orElseThrow();
    }

    private Binding binding(Declarables declarables, String routingKey) {
        return declarables.getDeclarables().stream()
                .filter(Declarable.class::isInstance)
                .map(Declarable.class::cast)
                .filter(Binding.class::isInstance)
                .map(Binding.class::cast)
                .filter(binding -> binding.getRoutingKey().equals(routingKey))
                .findFirst()
                .orElseThrow();
    }
}
