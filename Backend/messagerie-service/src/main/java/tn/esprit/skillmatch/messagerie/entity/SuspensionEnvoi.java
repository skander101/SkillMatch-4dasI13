package tn.esprit.skillmatch.messagerie.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "suspensions_envoi")
public class SuspensionEnvoi {

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "conversation_id", length = 36)
    private String conversationId;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "motif", columnDefinition = "TEXT")
    private String motif;

    @Column(name = "admin_id", nullable = false)
    private Long adminId;

    @Column(name = "date_debut", nullable = false)
    private LocalDateTime dateDebut;

    @Column(name = "date_fin")
    private LocalDateTime dateFin;

    public SuspensionEnvoi() {}

    public SuspensionEnvoi(String id, Long userId, String conversationId, String motif, Long adminId, LocalDateTime dateDebut) {
        this.id = id;
        this.userId = userId;
        this.conversationId = conversationId;
        this.active = true;
        this.motif = motif;
        this.adminId = adminId;
        this.dateDebut = dateDebut != null ? dateDebut : LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public String getMotif() { return motif; }
    public void setMotif(String motif) { this.motif = motif; }

    public Long getAdminId() { return adminId; }
    public void setAdminId(Long adminId) { this.adminId = adminId; }

    public LocalDateTime getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDateTime dateDebut) { this.dateDebut = dateDebut; }

    public LocalDateTime getDateFin() { return dateFin; }
    public void setDateFin(LocalDateTime dateFin) { this.dateFin = dateFin; }
}
