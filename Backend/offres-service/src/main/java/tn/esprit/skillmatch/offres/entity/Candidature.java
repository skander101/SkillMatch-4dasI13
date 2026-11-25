
package tn.esprit.skillmatch.offres.entity;
// Indique où se trouve notre classe dans le projet.

// Importe les outils pour travailler avec la base de données.
import jakarta.persistence.*;

// Permet de vérifier qu'une valeur n'est pas vide (null).
import jakarta.validation.constraints.NotNull;

// Permet de vérifier qu'un nombre est supérieur à 0.
import jakarta.validation.constraints.Positive;

// Importe les différents statuts d'une candidature.
import tn.esprit.skillmatch.offres.enums.CandidatureStatut;

// Permet de travailler avec la date et l'heure.
import java.time.LocalDateTime;

// Permet de vérifier qu'une valeur n'est pas null.
import java.util.Objects;


// Cette classe représente une table dans la base de données.
@Entity

// La table s'appelle "candidatures".
// Un candidat ne peut postuler qu'une fois à la même offre.
@Table(
    name = "candidatures",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_candidature_user_offre",
        columnNames = {"user_id", "offre_id"}
    )
)

public class Candidature {
    // On crée la classe Candidature.


    // ========== IDENTIFIANT ==========

    @Id // id est la clé primaire de notre table.

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // MySQL crée automatiquement un nouvel id : 1, 2, 3...

    private Long id;
    // Identifiant unique de chaque candidature.


    // ========== STATUT ==========

    @NotNull // Le statut ne doit pas être null.

    @Enumerated(EnumType.STRING)
    // On stocke "RECUE" en base au lieu d'un numéro.

    @Column(nullable = false)
    // Cette colonne est obligatoire dans MySQL.

    private CandidatureStatut statut = CandidatureStatut.RECUE;
    // Au début, chaque candidature a le statut RECUE.


    // ========== LETTRE DE MOTIVATION ==========

    @Column(columnDefinition = "TEXT")
    // Permet de stocker un texte long dans MySQL.

    private String lettre;
    // Lettre de motivation écrite par le candidat.


    // ========== DATE DE CANDIDATURE ==========

    @Column(nullable = false, updatable = false)
    // La date est obligatoire.
    // JPA ne la modifiera pas lors d'un UPDATE.

    private LocalDateTime dateCandidature;
    // Date et heure auxquelles le candidat a postulé.


    // ========== ENTRETIEN ==========

    private LocalDateTime dateEntretien;
    // Date et heure de l'entretien, si un entretien est prévu.

    private Integer noteRecruteur;
    // Note donnée par le recruteur au candidat.


    // ========== COMMENTAIRE RECRUTEUR ==========

    @Column(columnDefinition = "TEXT")
    // Permet d'enregistrer un commentaire long.

    private String commentaireRecruteur;
    // Avis écrit par le recruteur sur le candidat.


    // ========== IDENTIFIANT DU CANDIDAT ==========

    @NotNull // L'identifiant est obligatoire.

    @Positive // L'identifiant doit être supérieur à 0.

    @Column(nullable = false)
    // On ne peut pas enregistrer un userId null dans MySQL.

    private Long userId;
    // Identifiant du candidat qui a postulé.


    // ========== IDENTIFIANT DU CV ==========

    @Positive // Si un CV est indiqué, son id doit être positif.

    @Column(nullable = true)
    // Le CV est facultatif : on peut postuler sans CV.

    private Long cvId;
    // Identifiant du CV du candidat.
    // Il pourra venir du microservice Profils & CV.


    // ========== RELATION AVEC OFFRE ==========

    @NotNull // Une candidature doit avoir une offre.

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    // Plusieurs candidatures peuvent concerner une même offre.
    // LAZY : JPA charge l'offre quand on en a besoin.

    @JoinColumn(name = "offre_id", nullable = false)
    // La table candidatures contient une colonne offre_id.
    // Cette colonne relie la candidature à son offre.

    private Offre offre;
    // L'offre d'emploi à laquelle le candidat postule.
    // Supprimer une candidature ne supprime pas son offre.


    // ========== INITIALISATION AUTOMATIQUE ==========

    @PrePersist
        // Exécute cette méthode juste avant l'enregistrement
        // d'une nouvelle candidature dans la base.

    void initialiser() {

        dateCandidature = LocalDateTime.now();
        // Enregistre automatiquement la date et l'heure actuelles.

        statut = CandidatureStatut.RECUE;
        // Chaque nouvelle candidature commence avec le statut RECUE.
    }


    // ========== CHANGER LE STATUT ==========

    public void changerStatut(CandidatureStatut nouveau) {
        // Méthode qui permet de changer le statut.

        statut = Objects.requireNonNull(
            nouveau,
            "Le statut est obligatoire"
        );
        // Vérifie que le nouveau statut n'est pas null.
        // S'il est null, une erreur est déclenchée.
        // Sinon, le statut prend la nouvelle valeur.
    }


    // ========== GETTERS ET SETTERS ==========


    // ----- ID -----

    public Long getId() {
        // Getter : permet de récupérer l'id.
        return id;
        // Renvoie l'id de cette candidature.
    }

    public void setId(Long id) {
        // Setter : permet de donner une valeur à l'id.
        this.id = id;
        // this.id = l'id de l'objet.
        // id = la valeur reçue en paramètre.
    }


    // ----- STATUT -----

    public CandidatureStatut getStatut() {
        // Permet de connaître le statut de la candidature.
        return statut;
        // Renvoie par exemple RECUE.
    }

    public void setStatut(CandidatureStatut statut) {
        // Permet de modifier le statut.
        this.statut = statut;
        // Enregistre le statut reçu.
    }


    // ----- LETTRE DE MOTIVATION -----

    public String getLettre() {
        // Permet de lire la lettre de motivation.
        return lettre;
        // Renvoie le texte de la lettre.
    }

    public void setLettre(String lettre) {
        // Permet d'ajouter ou modifier la lettre.
        this.lettre = lettre;
        // Enregistre la lettre reçue.
    }


    // ----- DATE DE CANDIDATURE -----

    public LocalDateTime getDateCandidature() {
        // Permet de connaître la date de dépôt.
        return dateCandidature;
        // Renvoie la date et l'heure de candidature.
    }


    // ----- DATE D'ENTRETIEN -----

    public LocalDateTime getDateEntretien() {
        // Permet de connaître la date d'entretien.
        return dateEntretien;
        // Renvoie la date d'entretien.
    }

    public void setDateEntretien(LocalDateTime dateEntretien) {
        // Permet de fixer ou modifier la date d'entretien.
        this.dateEntretien = dateEntretien;
        // Enregistre la date reçue.
    }


    // ----- NOTE DU RECRUTEUR -----

    public Integer getNoteRecruteur() {
        // Permet de consulter la note du recruteur.
        return noteRecruteur;
        // Renvoie la note enregistrée.
    }

    public void setNoteRecruteur(Integer noteRecruteur) {
        // Permet au programme de définir une note.
        this.noteRecruteur = noteRecruteur;
        // Enregistre la nouvelle note.
    }


    // ----- COMMENTAIRE DU RECRUTEUR -----

    public String getCommentaireRecruteur() {
        // Permet de lire le commentaire du recruteur.
        return commentaireRecruteur;
        // Renvoie le commentaire.
    }

    public void setCommentaireRecruteur(String commentaireRecruteur) {
        // Permet d'ajouter ou modifier un commentaire.
        this.commentaireRecruteur = commentaireRecruteur;
        // Enregistre le commentaire reçu.
    }


    // ----- IDENTIFIANT DU CANDIDAT -----

    public Long getUserId() {
        // Permet de connaître l'identifiant du candidat.
        return userId;
        // Renvoie l'id du candidat.
    }

    public void setUserId(Long userId) {
        // Permet d'affecter un candidat à cette candidature.
        this.userId = userId;
        // Enregistre l'identifiant reçu.
    }


    // ----- IDENTIFIANT DU CV -----

    public Long getCvId() {
        // Permet de connaître l'identifiant du CV.
        return cvId;
        // Renvoie l'id du CV, ou null si aucun CV.
    }

    public void setCvId(Long cvId) {
        // Permet de renseigner un CV.
        this.cvId = cvId;
        // Enregistre l'identifiant du CV.
    }


    // ----- OFFRE ASSOCIÉE -----

    public Offre getOffre() {
        // Permet de récupérer l'offre associée.
        return offre;
        // Renvoie l'objet Offre.
    }

    public void setOffre(Offre offre) {
        // Permet d'associer une offre à la candidature.
        this.offre = offre;
        // Enregistre l'offre reçue.
    }

} // Fin de la classe Candidature.
