package tn.esprit.skillmatch.profils.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "messaging.enabled", havingValue = "true")
public class RabbitConfig {

    @Bean
    TopicExchange skillmatchExchange(@Value("${messaging.exchange}") String exchange) {
        return new TopicExchange(exchange, true, false);
    }

    @Bean
    Queue profilsTestPassedQueue(@Value("${messaging.queue}") String queue) {
        return new Queue(queue, true);
    }

    @Bean
    Binding profilsTestPassedBinding(Queue profilsTestPassedQueue, TopicExchange skillmatchExchange,
                                     @Value("${messaging.routing-key}") String routingKey) {
        return BindingBuilder.bind(profilsTestPassedQueue).to(skillmatchExchange).with(routingKey);
    }

    @Bean
    SimpleMessageListenerContainer profilsTestPassedContainer(
            ConnectionFactory connectionFactory,
            Queue profilsTestPassedQueue,
            MessageListenerAdapter profilsTestPassedListener
    ) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueues(profilsTestPassedQueue);
        container.setMessageListener(profilsTestPassedListener);
        return container;
    }

    @Bean
    MessageListenerAdapter profilsTestPassedListener(TestPassedConsumer consumer) {
        return new MessageListenerAdapter(consumer, "consommer");
    }
}
