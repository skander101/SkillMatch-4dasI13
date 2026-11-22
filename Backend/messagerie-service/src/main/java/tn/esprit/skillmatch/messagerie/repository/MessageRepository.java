package tn.esprit.skillmatch.messagerie.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.skillmatch.messagerie.entity.Message;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, String> {

    /** Messages d'une conversation, triés par date croissante. */
    List<Message> findByConversationIdOrderByDateAsc(String conversationId);
}
