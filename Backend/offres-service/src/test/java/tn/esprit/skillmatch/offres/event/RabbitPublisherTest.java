package tn.esprit.skillmatch.offres.event;

import org.junit.jupiter.api.*;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.*;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.transaction.*;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.*;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RabbitPublisherTest {
    private AnnotationConfigApplicationContext contexte;
    private RabbitTemplate rabbit;
    private final TransactionTemplate transaction = new TransactionTemplate(new TransactionTest());
    private final EvenementOffres evenement = new EvenementOffres(UUID.randomUUID(), "OfferPublished", Instant.now(), Map.of("offreId", 1));

    @BeforeEach
    void preparer() {
        contexte = new AnnotationConfigApplicationContext(Config.class);
        rabbit = contexte.getBean(RabbitTemplate.class);
        clearInvocations(rabbit); // Ignore les appels d'initialisation du bean par Spring.
    }

    @AfterEach
    void fermer() { contexte.close(); }

    @Test
    void attendLeCommitEtPrepareUnMessagePersistant() {
        doAnswer(appel -> {
            Message message = appel.<MessagePostProcessor>getArgument(3).postProcessMessage(new Message(new byte[0]));
            assertEquals(MessageDeliveryMode.PERSISTENT, message.getMessageProperties().getDeliveryMode());
            assertEquals(evenement.eventId().toString(), message.getMessageProperties().getMessageId());
            appel.<CorrelationData>getArgument(4).getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbit).convertAndSend(eq("test.events"), eq("OfferPublished"), eq(evenement), any(MessagePostProcessor.class), any(CorrelationData.class));
        transaction.executeWithoutResult(statut -> { contexte.publishEvent(evenement); verifyNoInteractions(rabbit); });
        verify(rabbit).convertAndSend(eq("test.events"), eq("OfferPublished"), eq(evenement), any(MessagePostProcessor.class), any(CorrelationData.class));
    }

    @Test
    void nePublieNiSansTransactionNiApresRollback() {
        contexte.publishEvent(evenement);
        transaction.executeWithoutResult(statut -> { contexte.publishEvent(evenement); statut.setRollbackOnly(); });
        verifyNoInteractions(rabbit);
    }

    @Test
    void unePanneRabbitNeTransformePasUnCommitEnErreurCrud() {
        doThrow(new org.springframework.amqp.AmqpException("Broker de test indisponible"))
                .when(rabbit).convertAndSend(anyString(), anyString(), any(), any(MessagePostProcessor.class), any(CorrelationData.class));
        assertDoesNotThrow(() -> transaction.executeWithoutResult(statut -> contexte.publishEvent(evenement)));
    }

    @TestConfiguration // Ce faux RabbitTemplate ne doit jamais entrer dans les tests avec un vrai broker.
    @EnableTransactionManagement
    @Import(RabbitPublisher.class)
    static class Config {
        @Bean RabbitTemplate rabbit() { return mock(RabbitTemplate.class); }
        @Bean TopicExchange exchange() { return new TopicExchange("test.events"); }
    }

    // Transaction sans base, uniquement pour vérifier les phases commit/rollback de Spring.
    static class TransactionTest extends AbstractPlatformTransactionManager {
        protected Object doGetTransaction() { return new Object(); }
        protected void doBegin(Object t, TransactionDefinition d) {}
        protected void doCommit(DefaultTransactionStatus s) {}
        protected void doRollback(DefaultTransactionStatus s) {}
    }
}
