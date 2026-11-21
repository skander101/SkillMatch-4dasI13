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
 * Table conversations dans messagerie_db.
 * Les identifiants des participants sont stockés en colonne JSON (List&lt;Long&gt;),
 * triés par ordre croissant pour permettre la détection de doublons.
 * Aucune clé étrangère vers commun_db.
 */
@Entity
@Table(name = "conversations")
public class Conversation {

    @Id
    @Column(nullable = false, updatable = false)
    private String id;

    /** Ids des participants, triés (détection de doublons). Stocké en JSON TEXT. */
    @Convert(converter = LongListConverter.class)
    @Column(nullable = false, columnDefinition = "TEXT")
    private List<Long> participantIds = new ArrayList<>();

    /** Id du candidat initiateur de la conversation. */
    @Column(nullable = false)
    private Long candidatUserId;

    @Column
    private LocalDateTime dernierMessageAt;

    public Conversation() {}

    public Conversation(String id, List<Long> participantIds, Long candidatUserId) {
        this.id = id;
        this.participantIds = participantIds;
        this.candidatUserId = candidatUserId;
    }

    public boolean estParticipant(Long userId) {
        return participantIds.contains(userId);
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public List<Long> getParticipantIds() { return participantIds; }
    public void setParticipantIds(List<Long> participantIds) { this.participantIds = participantIds; }

    public Long getCandidatUserId() { return candidatUserId; }
    public void setCandidatUserId(Long candidatUserId) { this.candidatUserId = candidatUserId; }

    public LocalDateTime getDernierMessageAt() { return dernierMessageAt; }
    public void setDernierMessageAt(LocalDateTime dernierMessageAt) { this.dernierMessageAt = dernierMessageAt; }
}
