package tn.esprit.skillmatch.offres.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import tn.esprit.skillmatch.offres.enums.CandidatureStatut;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "candidatures")
public class Candidature {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CandidatureStatut statut = CandidatureStatut.RECUE;

    @Column(columnDefinition = "TEXT")
    private String lettre;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dateCandidature;

    private LocalDateTime dateEntretien;
    private Integer noteRecruteur;

    @Column(columnDefinition = "TEXT")
    private String commentaireRecruteur;

    @NotNull
    @Positive
    @Column(nullable = false)
    private Long userId;

    @NotNull
    @Positive
    @Column(nullable = false)
    private Long cvId;

    // Aucune cascade : supprimer une candidature ne supprime jamais son offre.
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "offre_id", nullable = false)
    private Offre offre;

    @PrePersist
    void initialiser() {
        dateCandidature = LocalDateTime.now();
        statut = CandidatureStatut.RECUE;
    }

    public void changerStatut(CandidatureStatut nouveau) {
        statut = Objects.requireNonNull(nouveau, "Le statut est obligatoire");
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CandidatureStatut getStatut() {
        return statut;
    }

    public void setStatut(CandidatureStatut statut) {
        this.statut = statut;
    }

    public String getLettre() {
        return lettre;
    }

    public void setLettre(String lettre) {
        this.lettre = lettre;
    }

    public LocalDateTime getDateCandidature() {
        return dateCandidature;
    }

    public LocalDateTime getDateEntretien() {
        return dateEntretien;
    }

    public void setDateEntretien(LocalDateTime dateEntretien) {
        this.dateEntretien = dateEntretien;
    }

    public Integer getNoteRecruteur() {
        return noteRecruteur;
    }

    public void setNoteRecruteur(Integer noteRecruteur) {
        this.noteRecruteur = noteRecruteur;
    }

    public String getCommentaireRecruteur() {
        return commentaireRecruteur;
    }

    public void setCommentaireRecruteur(String commentaireRecruteur) {
        this.commentaireRecruteur = commentaireRecruteur;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getCvId() {
        return cvId;
    }

    public void setCvId(Long cvId) {
        this.cvId = cvId;
    }

    public Offre getOffre() {
        return offre;
    }

    public void setOffre(Offre offre) {
        this.offre = offre;
    }
}
