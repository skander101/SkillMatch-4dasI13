package tn.esprit.skillmatch.tests.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "app.rabbitmq.publication-enabled", havingValue = "true", matchIfMissing = true)
class TestEventsConfiguration {

    @Bean
    TopicExchange testEventsExchange(@Value("${app.rabbitmq.exchange:skillmatch.events}") String name) {
        return new TopicExchange(name, true, false);
    }

    @Bean
    RabbitTestPassedPublisher rabbitTestPassedPublisher(
            RabbitTemplate rabbitTemplate, TopicExchange testEventsExchange, ObjectMapper objectMapper
    ) {
        return new RabbitTestPassedPublisher(rabbitTemplate, testEventsExchange, objectMapper);
    }
}
