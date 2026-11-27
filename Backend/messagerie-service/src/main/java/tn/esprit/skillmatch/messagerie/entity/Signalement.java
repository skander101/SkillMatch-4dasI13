package tn.esprit.skillmatch.messagerie.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "signalements")
public class Signalement {

    public static final String STATUT_EN_ATTENTE = "EN_ATTENTE";
    public static final String STATUT_IGNORE = "IGNORE";
    public static final String STATUT_AVERTI = "AVERTI";
    public static final String STATUT_MESSAGE_MASQUE = "MESSAGE_MASQUE";
    public static final String STATUT_ENVOI_SUSPENDU = "ENVOI_SUSPENDU";

    public static final String ACTION_IGNORER = "IGNORER";
    public static final String ACTION_AVERTIR = "AVERTIR";
    public static final String ACTION_MASQUER_MESSAGE = "MASQUER_MESSAGE";
    public static final String ACTION_SUSPENDRE_ENVOI = "SUSPENDRE_ENVOI";

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "conversation_id", length = 36, nullable = false)
    private String conversationId;

    @Column(name = "message_id", length = 36, nullable = false)
    private String messageId;

    @Column(name = "auteur_signalement_id", nullable = false)
    private Long auteurSignalementId;

    @Column(name = "auteur_message_id", nullable = false)
    private Long auteurMessageId;

    @Column(name = "motif", length = 60, nullable = false)
    private String motif;

    @Column(name = "commentaire", columnDefinition = "TEXT")
    private String commentaire;

    @Column(name = "statut", length = 30, nullable = false)
    private String statut = STATUT_EN_ATTENTE;

    @Column(name = "action_admin", length = 40)
    private String actionAdmin;

    @Column(name = "admin_id")
    private Long adminId;

    @Column(name = "note_admin", columnDefinition = "TEXT")
    private String noteAdmin;

    @Column(name = "date_signalement", nullable = false)
    private LocalDateTime dateSignalement;

    @Column(name = "date_traitement")
    private LocalDateTime dateTraitement;

    public Signalement() {}

    public Signalement(String id, String conversationId, String messageId,
                       Long auteurSignalementId, Long auteurMessageId,
                       String motif, String commentaire, LocalDateTime dateSignalement) {
        this.id = id;
        this.conversationId = conversationId;
        this.messageId = messageId;
        this.auteurSignalementId = auteurSignalementId;
        this.auteurMessageId = auteurMessageId;
        this.motif = motif;
        this.commentaire = commentaire;
        this.statut = STATUT_EN_ATTENTE;
        this.dateSignalement = dateSignalement != null ? dateSignalement : LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public Long getAuteurSignalementId() { return auteurSignalementId; }
    public void setAuteurSignalementId(Long auteurSignalementId) { this.auteurSignalementId = auteurSignalementId; }

    public Long getAuteurMessageId() { return auteurMessageId; }
    public void setAuteurMessageId(Long auteurMessageId) { this.auteurMessageId = auteurMessageId; }

    public String getMotif() { return motif; }
    public void setMotif(String motif) { this.motif = motif; }

    public String getCommentaire() { return commentaire; }
    public void setCommentaire(String commentaire) { this.commentaire = commentaire; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public String getActionAdmin() { return actionAdmin; }
    public void setActionAdmin(String actionAdmin) { this.actionAdmin = actionAdmin; }

    public Long getAdminId() { return adminId; }
    public void setAdminId(Long adminId) { this.adminId = adminId; }

    public String getNoteAdmin() { return noteAdmin; }
    public void setNoteAdmin(String noteAdmin) { this.noteAdmin = noteAdmin; }

    public LocalDateTime getDateSignalement() { return dateSignalement; }
    public void setDateSignalement(LocalDateTime dateSignalement) { this.dateSignalement = dateSignalement; }

    public LocalDateTime getDateTraitement() { return dateTraitement; }
    public void setDateTraitement(LocalDateTime dateTraitement) { this.dateTraitement = dateTraitement; }
}
