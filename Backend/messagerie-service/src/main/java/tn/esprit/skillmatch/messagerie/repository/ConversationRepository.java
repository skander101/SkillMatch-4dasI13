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
     * Les virgules et crochets du JSON servent de délimiteurs pour éviter les faux positifs (1 vs 11).
     */
    @Query(value = "SELECT * FROM conversations WHERE participant_ids = CONCAT('[', :userId, ']') OR participant_ids LIKE CONCAT('[', :userId, ',%') OR participant_ids LIKE CONCAT('%,', :userId, ',%') OR participant_ids LIKE CONCAT('%,', :userId, ']') ORDER BY dernier_message_at DESC, date_creation DESC", nativeQuery = true)
    List<Conversation> findByParticipantId(@Param("userId") String userId);

    /**
     * Recherche par la chaîne JSON triée exacte des participantIds.
     * Utilisé pour détecter les doublons (même ensemble de participants).
     */
    @Query(value = "SELECT * FROM conversations WHERE participant_ids = :sortedJson", nativeQuery = true)
    Optional<Conversation> findByParticipantIds(@Param("sortedJson") String sortedJson);

    List<Conversation> findAllByOrderByDernierMessageAtDescDateCreationDesc();
}
