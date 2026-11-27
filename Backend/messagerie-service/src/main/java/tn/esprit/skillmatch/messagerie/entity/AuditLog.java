package tn.esprit.skillmatch.messagerie.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_messagerie")
public class AuditLog {

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "conversation_id", length = 36)
    private String conversationId;

    @Column(name = "message_id", length = 36)
    private String messageId;

    @Column(name = "signalement_id", length = 36)
    private String signalementId;

    @Column(name = "acteur_id", nullable = false)
    private Long acteurId;

    @Column(name = "acteur_nom", length = 150)
    private String acteurNom;

    @Column(name = "acteur_role", length = 30)
    private String acteurRole;

    @Column(name = "action", length = 60, nullable = false)
    private String action;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @Column(name = "date_action", nullable = false)
    private LocalDateTime dateAction;

    public AuditLog() {}

    public AuditLog(String id, String conversationId, String messageId, String signalementId,
                    Long acteurId, String acteurNom, String acteurRole,
                    String action, String details, LocalDateTime dateAction) {
        this.id = id;
        this.conversationId = conversationId;
        this.messageId = messageId;
        this.signalementId = signalementId;
        this.acteurId = acteurId;
        this.acteurNom = acteurNom;
        this.acteurRole = acteurRole;
        this.action = action;
        this.details = details;
        this.dateAction = dateAction != null ? dateAction : LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getSignalementId() { return signalementId; }
    public void setSignalementId(String signalementId) { this.signalementId = signalementId; }

    public Long getActeurId() { return acteurId; }
    public void setActeurId(Long acteurId) { this.acteurId = acteurId; }

    public String getActeurNom() { return acteurNom; }
    public void setActeurNom(String acteurNom) { this.acteurNom = acteurNom; }

    public String getActeurRole() { return acteurRole; }
    public void setActeurRole(String acteurRole) { this.acteurRole = acteurRole; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public LocalDateTime getDateAction() { return dateAction; }
    public void setDateAction(LocalDateTime dateAction) { this.dateAction = dateAction; }
}
