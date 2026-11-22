package tn.esprit.skillmatch.messagerie.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Table messages dans messagerie_db.
 * Clé étrangère messages.conversation_id → conversations.id (même base).
 * senderId vient toujours du JWT (via AuthenticatedUser), jamais du corps de la requête.
 */
@Entity
@Table(name = "messages")
public class Message {

    @Id
    @Column(nullable = false, updatable = false)
    private String id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String contenu;

    @Column(nullable = false)
    private LocalDateTime date;

    @Column(nullable = false)
    private Boolean lu = false;

    @ManyToOne(optional = false)
    @JoinColumn(name = "conversation_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_message_conversation"))
    private Conversation conversation;

    /** Id de l'expéditeur, issu du JWT via /auth/me. Jamais fourni par le client. */
    @Column(nullable = false)
    private Long senderId;

    public Message() {}

    public Message(String id, String contenu, LocalDateTime date, Conversation conversation, Long senderId) {
        this.id = id;
        this.contenu = contenu;
        this.date = date;
        this.conversation = conversation;
        this.senderId = senderId;
        this.lu = false;
    }

    public void marquerCommeLu() {
        this.lu = true;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }

    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }

    public Boolean getLu() { return lu; }
    public void setLu(Boolean lu) { this.lu = lu; }

    public Conversation getConversation() { return conversation; }
    public void setConversation(Conversation conversation) { this.conversation = conversation; }

    public Long getSenderId() { return senderId; }
    public void setSenderId(Long senderId) { this.senderId = senderId; }
}
