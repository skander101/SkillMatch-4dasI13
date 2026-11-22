package tn.esprit.skillmatch.messagerie.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.skillmatch.messagerie.entity.Conversation;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, String> {

    /**
     * Retrouve les conversations auxquelles un utilisateur participe.
     * Cherche la présence de l'id dans la colonne JSON TEXT (LIKE %userId%).
     * Convient pour MySQL et H2 (pas de fonction JSON nécessaire).
     */
    @Query("SELECT c FROM Conversation c WHERE c.participantIds LIKE CONCAT('%', :userId, '%') ORDER BY c.dernierMessageAt DESC")
    List<Conversation> findByParticipantId(@Param("userId") String userId);

    /**
     * Recherche par la chaîne JSON triée exacte des participantIds.
     * Utilisé pour détecter les doublons (même ensemble de participants).
     */
    @Query("SELECT c FROM Conversation c WHERE c.participantIds = :sortedJson")
    Optional<Conversation> findByParticipantIds(@Param("sortedJson") String sortedJson);
}
