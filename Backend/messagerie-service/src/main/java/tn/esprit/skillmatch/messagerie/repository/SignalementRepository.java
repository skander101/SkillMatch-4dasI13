package tn.esprit.skillmatch.messagerie.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.skillmatch.messagerie.entity.Signalement;

import java.util.List;

public interface SignalementRepository extends JpaRepository<Signalement, String> {

    List<Signalement> findByConversationIdOrderByDateSignalementDesc(String conversationId);

    List<Signalement> findByStatutOrderByDateSignalementDesc(String statut);

    List<Signalement> findAllByOrderByDateSignalementDesc();

    long countByStatut(String statut);

    long countByConversationIdAndStatut(String conversationId, String statut);

    long countByConversationId(String conversationId);

    boolean existsByMessageIdAndAuteurSignalementId(String messageId, Long auteurSignalementId);

    void deleteByConversationId(String conversationId);
}
