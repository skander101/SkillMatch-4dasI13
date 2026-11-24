package tn.esprit.skillmatch.messagerie.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.skillmatch.messagerie.entity.Message;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MessageRepository extends JpaRepository<Message, String> {

    /** Messages d'une conversation, triés par date croissante. */
    List<Message> findByConversationIdOrderByDateAsc(String conversationId);

    Optional<Message> findFirstByConversationIdOrderByDateDesc(String conversationId);

    long countByConversationId(String conversationId);

    long countByConversationIdAndLuFalseAndSenderIdNot(String conversationId, Long senderId);

    long countByDateGreaterThanEqualAndDateLessThan(LocalDateTime startInclusive, LocalDateTime endExclusive);

    List<Message> findByDateGreaterThanEqualAndDateLessThan(LocalDateTime startInclusive, LocalDateTime endExclusive);

    void deleteByConversationId(String conversationId);
}
