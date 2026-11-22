package tn.esprit.skillmatch.tests.events;

import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class RabbitTestPassedPublisherTest {

    private final ObjectMapper mapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();
    private final TestPassed event = new TestPassed(
            31L, 7L, 12L, "Java", 80, LocalDateTime.of(2026, 11, 16, 10, 30)
    );

    @Test
    void envoieLeContratJsonDurableSansTypeJava() throws Exception {
        var rabbit = new RecordingRabbitTemplate();
        var publisher = new RabbitTestPassedPublisher(rabbit, new TopicExchange("skillmatch.events"), mapper);

        publisher.publish(event);

        assertThat(rabbit.exchange).isEqualTo("skillmatch.events");
        assertThat(rabbit.routingKey).isEqualTo("TestPassed");
        var properties = rabbit.message.getMessageProperties();
        assertThat(properties.getContentType()).isEqualTo("application/json");
        assertThat(properties.getDeliveryMode()).isEqualTo(MessageDeliveryMode.PERSISTENT);
        assertThat(properties.getMessageId()).isEqualTo("TestPassed:31");
        assertThat(properties.getType()).isEqualTo("TestPassed");
        assertThat(properties.getHeaders()).doesNotContainKey("__TypeId__");
        assertThat(mapper.readTree(rabbit.message.getBody())).isEqualTo(mapper.readTree("""
                {
                  "eventId": "TestPassed:31",
                  "type": "TestPassed",
                  "occurredAt": "2026-11-16T10:30:00Z",
                  "source": "tests-service",
                  "data": {
                    "resultatId": 31,
                    "testId": 7,
                    "userId": 12,
                    "competence": "Java",
                    "score": 80,
                    "date": "2026-11-16T10:30:00"
                  }
                }
                """));
    }

    @Test
    void nePropagePasUnEchecDePublicationApresValidationDuResultat() {
        var unavailableRabbit = new RabbitTemplate() {
            @Override
            public void send(String exchange, String routingKey, Message message) {
                throw new AmqpException("RabbitMQ indisponible");
            }
        };
        var publisher = new RabbitTestPassedPublisher(
                unavailableRabbit, new TopicExchange("skillmatch.events"), mapper
        );

        assertThatCode(() -> publisher.publish(event)).doesNotThrowAnyException();
    }

    @Test
    void utiliseUnExchangeDurableEtConfigurable() {
        new ApplicationContextRunner()
                .withUserConfiguration(TestEventsConfiguration.class)
                .withBean(RabbitTemplate.class, RecordingRabbitTemplate::new)
                .withBean(ObjectMapper.class, () -> mapper)
                .withPropertyValues("app.rabbitmq.exchange=tests.integration")
                .run(context -> {
                    assertThat(context).hasSingleBean(RabbitTestPassedPublisher.class);
                    var exchange = context.getBean(TopicExchange.class);
                    assertThat(exchange.getName()).isEqualTo("tests.integration");
                    assertThat(exchange.getType()).isEqualTo("topic");
                    assertThat(exchange.isDurable()).isTrue();
                    assertThat(exchange.isAutoDelete()).isFalse();
                });
    }

    @Test
    void permetDeDesactiverLaPublicationLocale() {
        new ApplicationContextRunner()
                .withUserConfiguration(TestEventsConfiguration.class)
                .withPropertyValues("app.rabbitmq.publication-enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(RabbitTestPassedPublisher.class);
                    assertThat(context).doesNotHaveBean(TopicExchange.class);
                });
    }

    private static final class RecordingRabbitTemplate extends RabbitTemplate {

        private String exchange;
        private String routingKey;
        private Message message;

        RecordingRabbitTemplate() {
            super(new CachingConnectionFactory("localhost"));
        }

        @Override
        public void send(String exchange, String routingKey, Message message) {
            this.exchange = exchange;
            this.routingKey = routingKey;
            this.message = message;
        }
    }
}
