package tn.esprit.skillmatch.messagerie.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration RabbitMQ.
 * Exchange et queue conformes au contrat Backend/README.md :
 *   exchange = skillmatch.events (TopicExchange, partagé par tous les services)
 *   queue    = skillmatch.messagerie.message-sent
 *   routing  = messagerie.message.sent
 */
@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "skillmatch.events";
    public static final String QUEUE = "skillmatch.messagerie.message-sent";
    public static final String ROUTING_KEY = "messagerie.message.sent";

    @Bean
    public TopicExchange skillmatchExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Queue messageSentQueue() {
        return new Queue(QUEUE, true);
    }

    @Bean
    public Binding messageSentBinding(Queue messageSentQueue, TopicExchange skillmatchExchange) {
        return BindingBuilder.bind(messageSentQueue).to(skillmatchExchange).with(ROUTING_KEY);
    }

    @Bean
    public MessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
