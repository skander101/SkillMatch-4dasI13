package tn.esprit.skillmatch.alertes.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Exchange topic skillmatch.events et file alertes.offer-published (une file par consommateur). */
@Configuration
@ConditionalOnProperty(name = "alertes.messaging.enabled", havingValue = "true")
public class RabbitConfig {

    @Bean
    public TopicExchange skillmatchExchange(@Value("${alertes.messaging.exchange}") String exchange) {
        return new TopicExchange(exchange, true, false);
    }

    @Bean
    public Queue offerPublishedQueue(@Value("${alertes.messaging.queue-offer-published}") String queue) {
        return QueueBuilder.durable(queue).build();
    }

    @Bean
    public Binding offerPublishedBinding(Queue offerPublishedQueue, TopicExchange skillmatchExchange,
                                         @Value("${alertes.messaging.routing-key-offer-published}") String routingKey) {
        return BindingBuilder.bind(offerPublishedQueue).to(skillmatchExchange).with(routingKey);
    }

    /** Messages JSON ; le type est déduit du paramètre de la méthode @RabbitListener. */
    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
