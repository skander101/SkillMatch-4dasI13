package tn.esprit.skillmatch.offres.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.*;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tn.esprit.skillmatch.offres.entity.*;
import tn.esprit.skillmatch.offres.enums.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "eureka.client.enabled=false")
@EnabledIfEnvironmentVariable(named = "RABBITMQ_IT", matches = "true")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RabbitPublisherIT { // Nécessite MySQL, JWT_SECRET et un vrai RabbitMQ.
    private static final String PREFIXE = "skillmatch.test." + UUID.randomUUID();
    private static final List<String> TYPES = List.of("OfferPublished", "ApplicationSubmitted", "StatusChanged");
    @Autowired RabbitTemplate rabbit;
    @Autowired RabbitAdmin admin;
    @Autowired ObjectMapper json;
    @Autowired ApplicationEventPublisher evenements;
    @Autowired PlatformTransactionManager transactions;

    @DynamicPropertySource
    static void isoler(DynamicPropertyRegistry proprietes) {
        proprietes.add("skillmatch.events.exchange", () -> PREFIXE);
        proprietes.add("skillmatch.events.queue-prefix", () -> PREFIXE);
    }

    @Test
    void recoitLesTroisJsonUniquementApresCommit() throws Exception {
        admin.initialize();
        TransactionTemplate transaction = new TransactionTemplate(transactions);
        Offre offre = new Offre();
        offre.setId(1L); offre.setRecruteurId(2L); offre.setTitre("Offre de test");
        offre.setContrat(ContratType.CDI); offre.setDateExpiration(LocalDate.now().plusDays(10));
        offre.setCompetences(List.of("Java"));
        Candidature candidature = new Candidature();
        candidature.setId(3L); candidature.setOffre(offre); candidature.setUserId(4L);
        candidature.setLettre("Cette lettre ne doit pas être publiée");
        candidature.setStatut(CandidatureStatut.ACCEPTEE);
        candidature.setDateEntretien(LocalDateTime.now().plusDays(1));
        for (var evenement : List.of(EvenementOffres.offrePubliee(offre), EvenementOffres.candidatureDeposee(candidature),
                EvenementOffres.statutChange(candidature, CandidatureStatut.RECUE))) {
            String type = evenement.type();
            transaction.executeWithoutResult(statut -> {
                evenements.publishEvent(evenement);
                assertNull(rabbit.receive(PREFIXE + "." + type)); // Pas encore validé.
            });
            Message message = rabbit.receive(PREFIXE + "." + type, 5000);
            assertNotNull(message);
            assertEquals("application/json", message.getMessageProperties().getContentType());
            var corps = json.readTree(message.getBody());
            assertEquals(type, corps.get("type").asText());
            assertEquals(evenement.eventId().toString(), corps.get("eventId").asText());
            assertEquals(1, corps.get("donnees").get("offreId").asInt());
            assertFalse(corps.get("donnees").has("lettre"));
            transaction.executeWithoutResult(statut -> { evenements.publishEvent(evenement); statut.setRollbackOnly(); });
            assertNull(rabbit.receive(PREFIXE + "." + type, 200));
        }
    }

    @AfterAll
    void nettoyer() { // Supprime uniquement les files et l'exchange uniques de CE test.
        for (String type : TYPES) admin.deleteQueue(PREFIXE + "." + type);
        admin.deleteExchange(PREFIXE);
    }
}
