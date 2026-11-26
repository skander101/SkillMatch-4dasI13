package tn.esprit.skillmatch.profils.messaging;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RabbitConfigTest {

    private final RabbitConfig config = new RabbitConfig();

    @Test
    void eventInfrastructureUsesTopicExchange() {
        TopicExchange exchange = config.skillmatchExchange("skillmatch.events");
        Queue queue = config.profilsTestPassedQueue("profils.test-passed");
        Binding binding = config.profilsTestPassedBinding(queue, exchange, "test.passed");

        assertEquals("skillmatch.events", exchange.getName());
        assertEquals("profils.test-passed", queue.getName());
        assertEquals("test.passed", binding.getRoutingKey());
    }
}
