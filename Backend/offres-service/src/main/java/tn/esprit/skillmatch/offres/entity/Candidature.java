package tn.esprit.skillmatch.offres.entity; // Place ce fichier dans le package indiqué, qui organise les classes du service.

import jakarta.persistence.*; // Importe les annotations JPA pour les tables, colonnes et relations.
import jakarta.validation.constraints.NotNull; // Importe la validation qui refuse une valeur absente.
import jakarta.validation.constraints.Positive; // Importe la validation qui exige un nombre supérieur à zéro.
import tn.esprit.skillmatch.offres.enums.CandidatureStatut; // Importe les valeurs autorisées de CandidatureStatut.

import java.time.LocalDateTime; // Importe les dates avec l'heure.
import java.util.Objects; // Importe les vérifications simples comme le refus de null.

@Entity // Indique à JPA que cette classe est enregistrée dans une table.
@Table(name = "candidatures", uniqueConstraints = @UniqueConstraint(name = "uk_candidature_user_offre", columnNames = {"user_id", "offre_id"})) // Une seule candidature par candidat et offre, même après retrait.
public class Candidature { // Déclare l'entité Candidature, avec ses champs et ses méthodes.
    @Id // Désigne la clé primaire de la table.
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Laisse MySQL générer l'identifiant avec AUTO_INCREMENT.
    private Long id; // Déclare l'identifiant.

    @NotNull // Refuse une valeur null lors de la validation.
    @Enumerated(EnumType.STRING) // Enregistre le nom du statut ou du contrat en base, plutôt que son numéro.
    @Column(nullable = false) // Interdit NULL dans cette colonne de la base.
    private CandidatureStatut statut = CandidatureStatut.RECUE; // Déclare le statut ; RECUE est la valeur initiale.

    @Column(columnDefinition = "TEXT") // Utilise une colonne TEXT pour stocker un texte long.
    private String lettre; // Déclare la lettre de motivation.

    @Column(nullable = false, updatable = false) // Rend la date obligatoire en base et exclut sa modification des UPDATE JPA.
    private LocalDateTime dateCandidature; // Déclare la date de dépôt.

    private LocalDateTime dateEntretien; // Déclare la date d'entretien.
    private Integer noteRecruteur; // Déclare la note donnée par le recruteur.

    @Column(columnDefinition = "TEXT") // Utilise une colonne TEXT pour stocker un texte long.
    private String commentaireRecruteur; // Déclare le commentaire du recruteur.

    @NotNull // Refuse une valeur null lors de la validation.
    @Positive // Exige un identifiant strictement supérieur à zéro s'il est renseigné.
    @Column(nullable = false) // Interdit NULL dans cette colonne de la base.
    private Long userId; // Déclare l'identifiant du candidat.

    @NotNull // Refuse une valeur null lors de la validation.
    @Positive // Exige un identifiant strictement supérieur à zéro s'il est renseigné.
    @Column(nullable = false) // Interdit NULL dans cette colonne de la base.
    private Long cvId; // Déclare l'identifiant du CV.

    // Aucune cascade : supprimer une candidature ne supprime jamais son offre.
    @NotNull // Refuse une valeur null lors de la validation.
    @ManyToOne(fetch = FetchType.LAZY, optional = false) // Relie plusieurs candidatures à une offre obligatoire, chargée à la demande.
    @JoinColumn(name = "offre_id", nullable = false) // Stocke la clé étrangère obligatoire vers l'offre dans offre_id.
    private Offre offre; // Déclare l'offre associée.

    @PrePersist // Exécute la méthode suivante avant la première insertion JPA.
    void initialiser() { // Prépare les valeurs automatiques d'une nouvelle candidature.
        dateCandidature = LocalDateTime.now(); // Fixe la date et l'heure du dépôt au moment de l'insertion.
        statut = CandidatureStatut.RECUE; // Force le statut initial d'une nouvelle candidature à RECUE.
    } // Ferme le bloc précédent (méthode, condition ou classe).

    public void changerStatut(CandidatureStatut nouveau) { // Permet de changer le statut ; les droits seront contrôlés dans le service métier.
        statut = Objects.requireNonNull(nouveau, "Le statut est obligatoire"); // Affecte le nouveau statut ou refuse une valeur null.
    } // Ferme le bloc précédent (méthode, condition ou classe).

    public Long getId() { // Permet de lire l'identifiant.
        return id; // Retourne l'identifiant de cet objet.
    } // Ferme le bloc précédent (méthode, condition ou classe).

    public void setId(Long id) { // Permet de renseigner ou modifier l'identifiant.
        this.id = id; // Stocke la valeur reçue dans le champ de cet objet.
    } // Ferme le bloc précédent (méthode, condition ou classe).

    public CandidatureStatut getStatut() { // Permet de lire le statut.
        return statut; // Retourne le statut de cet objet.
    } // Ferme le bloc précédent (méthode, condition ou classe).

    public void setStatut(CandidatureStatut statut) { // Permet de renseigner ou modifier le statut.
        this.statut = statut; // Stocke la valeur reçue dans le champ de cet objet.
    } // Ferme le bloc précédent (méthode, condition ou classe).

    public String getLettre() { // Permet de lire la lettre de motivation.
        return lettre; // Retourne la lettre de motivation de cet objet.
    } // Ferme le bloc précédent (méthode, condition ou classe).

    public void setLettre(String lettre) { // Permet de renseigner ou modifier la lettre de motivation.
        this.lettre = lettre; // Stocke la valeur reçue dans le champ de cet objet.
    } // Ferme le bloc précédent (méthode, condition ou classe).

    public LocalDateTime getDateCandidature() { // Permet de lire la date de dépôt.
        return dateCandidature; // Retourne la date de dépôt de cet objet.
    } // Ferme le bloc précédent (méthode, condition ou classe).

    public LocalDateTime getDateEntretien() { // Permet de lire la date d'entretien.
        return dateEntretien; // Retourne la date d'entretien de cet objet.
    } // Ferme le bloc précédent (méthode, condition ou classe).

    public void setDateEntretien(LocalDateTime dateEntretien) { // Permet de renseigner ou modifier la date d'entretien.
        this.dateEntretien = dateEntretien; // Stocke la valeur reçue dans le champ de cet objet.
    } // Ferme le bloc précédent (méthode, condition ou classe).

    public Integer getNoteRecruteur() { // Permet de lire la note donnée par le recruteur.
        return noteRecruteur; // Retourne la note donnée par le recruteur de cet objet.
    } // Ferme le bloc précédent (méthode, condition ou classe).

    public void setNoteRecruteur(Integer noteRecruteur) { // Permet de renseigner ou modifier la note donnée par le recruteur.
        this.noteRecruteur = noteRecruteur; // Stocke la valeur reçue dans le champ de cet objet.
    } // Ferme le bloc précédent (méthode, condition ou classe).

    public String getCommentaireRecruteur() { // Permet de lire le commentaire du recruteur.
        return commentaireRecruteur; // Retourne le commentaire du recruteur de cet objet.
    } // Ferme le bloc précédent (méthode, condition ou classe).

    public void setCommentaireRecruteur(String commentaireRecruteur) { // Permet de renseigner ou modifier le commentaire du recruteur.
        this.commentaireRecruteur = commentaireRecruteur; // Stocke la valeur reçue dans le champ de cet objet.
    } // Ferme le bloc précédent (méthode, condition ou classe).

    public Long getUserId() { // Permet de lire l'identifiant du candidat.
        return userId; // Retourne l'identifiant du candidat de cet objet.
    } // Ferme le bloc précédent (méthode, condition ou classe).

    public void setUserId(Long userId) { // Permet de renseigner ou modifier l'identifiant du candidat.
        this.userId = userId; // Stocke la valeur reçue dans le champ de cet objet.
    } // Ferme le bloc précédent (méthode, condition ou classe).

    public Long getCvId() { // Permet de lire l'identifiant du CV.
        return cvId; // Retourne l'identifiant du CV de cet objet.
    } // Ferme le bloc précédent (méthode, condition ou classe).

    public void setCvId(Long cvId) { // Permet de renseigner ou modifier l'identifiant du CV.
        this.cvId = cvId; // Stocke la valeur reçue dans le champ de cet objet.
    } // Ferme le bloc précédent (méthode, condition ou classe).

    public Offre getOffre() { // Permet de lire l'offre associée.
        return offre; // Retourne l'offre associée de cet objet.
    } // Ferme le bloc précédent (méthode, condition ou classe).

    public void setOffre(Offre offre) { // Permet de renseigner ou modifier l'offre associée.
        this.offre = offre; // Stocke la valeur reçue dans le champ de cet objet.
    } // Ferme le bloc précédent (méthode, condition ou classe).
} // Ferme le bloc précédent (méthode, condition ou classe).
