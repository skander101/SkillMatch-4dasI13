package tn.esprit.skillmatch.messagerie.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.skillmatch.messagerie.entity.AuditLog;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, String> {

    List<AuditLog> findByConversationIdOrderByDateActionDesc(String conversationId);

    List<AuditLog> findAllByOrderByDateActionDesc();

    void deleteByConversationId(String conversationId);
}
