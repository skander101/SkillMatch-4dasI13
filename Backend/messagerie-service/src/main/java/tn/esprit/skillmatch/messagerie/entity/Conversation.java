package tn.esprit.skillmatch.messagerie.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Table conversations dans messagerie-service.
 * Les identifiants des participants sont stockés en colonne JSON (List<Long>),
 * triés par ordre croissant pour permettre la détection de doublons.
 * Aucune clé étrangère vers commun_db.
 */
@Entity
@Table(name = "conversations")
public class Conversation {

    public enum Type {
        DIRECT, GROUPE
    }

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    /** Ids des participants, triés (détection de doublons). Stocké en JSON TEXT. */
    @Convert(converter = LongListConverter.class)
    @Column(name = "participant_ids", nullable = false, columnDefinition = "TEXT")
    private List<Long> participantIds = new ArrayList<>();

    /** Ordre d'arrivée des participants dans la conversation (pour déterminer le plus ancien membre). */
    @Convert(converter = LongListConverter.class)
    @Column(name = "ordre_participants", columnDefinition = "TEXT")
    private List<Long> ordreParticipants = new ArrayList<>();

    /** Id du créateur de la conversation. */
    @Column(name = "candidat_user_id", nullable = false)
    private Long candidatUserId;

    /** Nom du groupe (uniquement si type = GROUPE, nullable sinon). */
    @Column(name = "nom_groupe", length = 100)
    private String nomGroupe;

    /** Type de conversation : DIRECT (1 contre 1) ou GROUPE. */
    @Column(name = "type_conversation", nullable = false, length = 10)
    private String type = Type.DIRECT.name();

    @Column(name = "dernier_message_at")
    private LocalDateTime dernierMessageAt;

    @Column(name = "archivee", nullable = false)
    private Boolean archivee = false;

    @Column(name = "date_archivage")
    private LocalDateTime dateArchivage;

    @Column(name = "signalee", nullable = false)
    private Boolean signalee = false;

    @Column(name = "date_signalement")
    private LocalDateTime dateSignalement;

    @Column(name = "date_creation")
    private LocalDateTime dateCreation;

    public Conversation() {}

    public Conversation(String id, List<Long> participantIds, Long candidatUserId) {
        this.id = id;
        this.participantIds = participantIds != null ? new ArrayList<>(participantIds) : new ArrayList<>();
        this.ordreParticipants = new ArrayList<>(this.participantIds);
        this.candidatUserId = candidatUserId;
        this.type = Type.DIRECT.name();
        this.dateCreation = LocalDateTime.now();
    }

    public Conversation(String id, List<Long> participantIds, Long candidatUserId,
                        String nomGroupe, Type type) {
        this.id = id;
        this.participantIds = participantIds != null ? new ArrayList<>(participantIds) : new ArrayList<>();
        this.ordreParticipants = new ArrayList<>(this.participantIds);
        this.candidatUserId = candidatUserId;
        this.nomGroupe = nomGroupe;
        this.type = type != null ? type.name() : Type.DIRECT.name();
        this.dateCreation = LocalDateTime.now();
    }

    public boolean estParticipant(Long userId) {
        return participantIds.contains(userId);
    }

    public boolean estGroupe() {
        return Type.GROUPE.name().equalsIgnoreCase(type);
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public List<Long> getParticipantIds() { return participantIds; }
    public void setParticipantIds(List<Long> participantIds) { this.participantIds = participantIds; }

    public List<Long> getOrdreParticipants() { return ordreParticipants; }
    public void setOrdreParticipants(List<Long> ordreParticipants) { this.ordreParticipants = ordreParticipants; }

    public Long getCandidatUserId() { return candidatUserId; }
    public void setCandidatUserId(Long candidatUserId) { this.candidatUserId = candidatUserId; }

    public String getNomGroupe() { return nomGroupe; }
    public void setNomGroupe(String nomGroupe) { this.nomGroupe = nomGroupe; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public LocalDateTime getDernierMessageAt() { return dernierMessageAt; }
    public void setDernierMessageAt(LocalDateTime dernierMessageAt) { this.dernierMessageAt = dernierMessageAt; }

    public Boolean getArchivee() { return archivee; }
    public void setArchivee(Boolean archivee) { this.archivee = archivee; }

    public LocalDateTime getDateArchivage() { return dateArchivage; }
    public void setDateArchivage(LocalDateTime dateArchivage) { this.dateArchivage = dateArchivage; }

    public Boolean getSignalee() { return signalee; }
    public void setSignalee(Boolean signalee) { this.signalee = signalee; }

    public LocalDateTime getDateSignalement() { return dateSignalement; }
    public void setDateSignalement(LocalDateTime dateSignalement) { this.dateSignalement = dateSignalement; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }
}
