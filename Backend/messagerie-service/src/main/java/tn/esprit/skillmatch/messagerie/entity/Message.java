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

    public static final String TYPE_TEXTE = "texte";
    public static final String TYPE_SYSTEME = "systeme";
    public static final String TYPE_FICHIER = "fichier";

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String contenu;

    @Column(name = "type_message", nullable = false, length = 20)
    private String type = TYPE_TEXTE;

    @Column(nullable = false)
    private LocalDateTime date;

    @Column(nullable = false)
    private Boolean lu = false;

    @ManyToOne(optional = false)
    @JoinColumn(name = "conversation_id", nullable = false, columnDefinition = "varchar(36)",
                foreignKey = @ForeignKey(name = "fk_message_conversation"))
    private Conversation conversation;

    /** Id de l'expéditeur, issu du JWT via /auth/me. Jamais fourni par le client. */
    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(name = "fichier_nom", length = 255)
    private String fichierNom;

    @Column(name = "fichier_url", length = 500)
    private String fichierUrl;

    @Column(name = "fichier_taille")
    private Long fichierTaille;

    @Column(name = "signalee", nullable = false)
    private Boolean signalee = false;

    @Column(name = "masque", nullable = false)
    private Boolean masque = false;

    public Message() {}

    public Message(String id, String contenu, LocalDateTime date, Conversation conversation, Long senderId) {
        this(id, contenu, TYPE_TEXTE, date, false, conversation, senderId, null, null, null);
    }

    public Message(String id, String contenu, String type, LocalDateTime date, Boolean lu,
                   Conversation conversation, Long senderId,
                   String fichierNom, String fichierUrl, Long fichierTaille) {
        this.id = id;
        this.contenu = contenu;
        this.type = normalizeType(type);
        this.date = date;
        this.lu = lu != null ? lu : false;
        this.conversation = conversation;
        this.senderId = senderId;
        this.fichierNom = fichierNom;
        this.fichierUrl = fichierUrl;
        this.fichierTaille = fichierTaille;
    }

    public static String normalizeType(String rawType) {
        if (rawType == null || rawType.isBlank()) {
            return TYPE_TEXTE;
        }
        String lower = rawType.trim().toLowerCase();
        if (TYPE_SYSTEME.equals(lower) || "système".equals(lower)) {
            return TYPE_SYSTEME;
        }
        if (TYPE_FICHIER.equals(lower)) {
            return TYPE_FICHIER;
        }
        return TYPE_TEXTE;
    }

    public boolean estSysteme() {
        return TYPE_SYSTEME.equalsIgnoreCase(type);
    }

    public void marquerCommeLu() {
        this.lu = true;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }

    public String getType() { return type; }
    public void setType(String type) { this.type = normalizeType(type); }

    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }

    public Boolean getLu() { return lu; }
    public void setLu(Boolean lu) { this.lu = lu; }

    public Conversation getConversation() { return conversation; }
    public void setConversation(Conversation conversation) { this.conversation = conversation; }

    public Long getSenderId() { return senderId; }
    public void setSenderId(Long senderId) { this.senderId = senderId; }

    public String getFichierNom() { return fichierNom; }
    public void setFichierNom(String fichierNom) { this.fichierNom = fichierNom; }

    public String getFichierUrl() { return fichierUrl; }
    public void setFichierUrl(String fichierUrl) { this.fichierUrl = fichierUrl; }

    public Long getFichierTaille() { return fichierTaille; }
    public void setFichierTaille(Long fichierTaille) { this.fichierTaille = fichierTaille; }

    public Boolean getSignalee() { return signalee; }
    public void setSignalee(Boolean signalee) { this.signalee = signalee; }

    public Boolean getMasque() { return masque; }
    public void setMasque(Boolean masque) { this.masque = masque; }
}
