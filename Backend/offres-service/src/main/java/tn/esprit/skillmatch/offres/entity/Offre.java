
package tn.esprit.skillmatch.offres.entity;
// Cette classe se trouve dans le package entity du microservice.

import jakarta.persistence.*;
// Importe les annotations pour travailler avec MySQL grâce à JPA.

import jakarta.validation.constraints.*;
// Importe les règles de validation des champs (@NotNull, @Size...).

import tn.esprit.skillmatch.offres.enums.ContratType;
// Importe les types de contrat autorisés (CDI, CDD...).

import tn.esprit.skillmatch.offres.enums.OffreStatut;
// Importe les différents statuts possibles d'une offre.

import java.time.LocalDate;
// Permet de stocker une date sans heure.

import java.util.ArrayList;
// Permet de créer une liste modifiable.

import java.util.List;
// Permet de déclarer une liste de compétences.


// ========== CLASSE OFFRE ==========

@Entity
// Indique que cette classe correspond à une table de la base.

@Table(name = "offres")
// La table MySQL s'appelle "offres".

public class Offre {
    // Début de la classe qui représente une offre d'emploi.


    // ========== IDENTIFIANT ==========

    @Id
    // Ce champ est la clé primaire de la table.

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // MySQL génère automatiquement l'id : 1, 2, 3...

    private Long id;
    // Identifiant unique de l'offre.


    // ========== TITRE ==========

    @NotBlank
    // Le titre ne doit pas être vide ou composé uniquement d'espaces.

    @Size(max = 255)
    // Le titre ne doit pas dépasser 255 caractères.

    @Column(nullable = false)
    // Le titre est obligatoire dans la base de données.

    private String titre;
    // Exemple : "Développeur Java".


    // ========== DESCRIPTION ==========

    @Column(columnDefinition = "TEXT")
    // Utilise une colonne TEXT pour un texte long.

    private String description;
    // Présentation du poste et des missions.


    // ========== COMPÉTENCES ==========

    @NotNull
    // La liste ne doit pas être null.

    @Convert(converter = CompetencesConverter.class)
    // Utilise CompetencesConverter pour enregistrer la liste en base.

    @Column(columnDefinition = "TEXT", nullable = false)
    // Enregistre les compétences dans une colonne TEXT obligatoire.

    private List<@NotBlank String> competences = new ArrayList<>();
    // Liste des compétences demandées : ["Java", "MySQL", "Spring"].
    // @NotBlank : chaque compétence doit contenir du texte.
    // new ArrayList<>() : la liste est vide au départ.


    // ========== LOCALISATION ==========

    @Size(max = 255)
    // Le nom du lieu ne doit pas dépasser 255 caractères.

    private String localisation;
    // Ville ou lieu du travail : "Tunis", "Sfax"...


    // ========== CONTRAT ==========

    @NotNull
    // Le type de contrat est obligatoire.

    @Enumerated(EnumType.STRING)
    // Enregistre "CDI" plutôt qu'un numéro dans MySQL.

    @Column(nullable = false)
    // La colonne contrat est obligatoire.

    private ContratType contrat;
    // Type de contrat choisi parmi les valeurs de ContratType.


    // ========== SALAIRE ==========

    @PositiveOrZero
    // Si un salaire est donné, il doit être supérieur ou égal à 0.

    private Double salaire;
    // Salaire proposé ; il peut aussi ne pas être renseigné.


    // ========== DATE D'EXPIRATION ==========

    @NotNull
    // La date doit être renseignée.

    @Column(nullable = false)
    // La date est obligatoire dans MySQL.

    private LocalDate dateExpiration;
    // Date limite de validité de l'offre.


    // ========== STATUT ==========

    @NotNull
    // Le statut ne doit pas être null.

    @Enumerated(EnumType.STRING)
    // Enregistre le nom du statut, par exemple "BROUILLON".

    @Column(nullable = false)
    // Le statut est obligatoire en base.

    private OffreStatut statut = OffreStatut.BROUILLON;
    // Une nouvelle offre commence avec le statut BROUILLON.


    // ========== NOM DE L'ENTREPRISE ==========

    @Size(max = 255)
    // Maximum 255 caractères si un nom est donné.

    private String nomEntreprise;
    // Nom de l'entreprise saisi sous forme de texte.


    // ========== IDENTIFIANT ENTREPRISE ==========

    @Positive
    // Si l'identifiant existe, il doit être supérieur à zéro.

    private Long entrepriseId;
    // Identifiant facultatif de l'entreprise.


    // ========== TEST TECHNIQUE ==========

    @Positive
    // Si l'identifiant du test existe, il doit être positif.

    private Long testRequisId;
    // Identifiant du test technique éventuellement demandé.


    // ========== RECRUTEUR ==========

    // Le propriétaire de l'offre est un recruteur (utilisateur).

    @NotNull
    // Le recruteur doit être renseigné.

    @Positive
    // Son identifiant doit être supérieur à zéro.

    @Column(nullable = false)
    // La colonne recruteurId est obligatoire dans MySQL.

    private Long recruteurId;
    // Identifiant du recruteur qui possède cette offre.


    // ========== GETTERS ET SETTERS ==========
    // GETTER : lire une valeur.
    // SETTER : enregistrer ou modifier une valeur.


    // ----- IDENTIFIANT -----

    public Long getId() {
        // Permet de lire l'identifiant.
        return id;
        // Renvoie l'id de l'offre.
    }

    public void setId(Long id) {
        // Permet de donner un identifiant à l'objet.
        this.id = id;
        // Stocke la valeur reçue dans l'attribut id.
    }


    // ----- TITRE -----

    public String getTitre() {
        // Permet de lire le titre de l'offre.
        return titre;
        // Renvoie le titre.
    }

    public void setTitre(String titre) {
        // Permet de modifier le titre.
        this.titre = titre;
        // Stocke le nouveau titre dans l'objet.
    }


    // ----- DESCRIPTION -----

    public String getDescription() {
        // Permet de lire la description.
        return description;
        // Renvoie la description.
    }

    public void setDescription(String description) {
        // Permet de modifier la description.
        this.description = description;
        // Stocke la description reçue.
    }


    // ----- COMPÉTENCES -----

    public List<String> getCompetences() {
        // Permet de consulter les compétences demandées.
        return competences;
        // Renvoie la liste des compétences.
    }

    public void setCompetences(List<String> competences) {
        // Permet de changer les compétences demandées.
        this.competences = competences;
        // Stocke la liste reçue.
    }


    // ----- LOCALISATION -----

    public String getLocalisation() {
        // Permet de connaître le lieu de travail.
        return localisation;
        // Renvoie la localisation.
    }

    public void setLocalisation(String localisation) {
        // Permet de changer le lieu de travail.
        this.localisation = localisation;
        // Stocke la nouvelle localisation.
    }


    // ----- CONTRAT -----

    public ContratType getContrat() {
        // Permet de connaître le type de contrat.
        return contrat;
        // Renvoie le contrat.
    }

    public void setContrat(ContratType contrat) {
        // Permet de modifier le contrat.
        this.contrat = contrat;
        // Stocke le nouveau contrat.
    }


    // ----- SALAIRE -----

    public Double getSalaire() {
        // Permet de lire le salaire proposé.
        return salaire;
        // Renvoie le salaire.
    }

    public void setSalaire(Double salaire) {
        // Permet de modifier le salaire.
        this.salaire = salaire;
        // Stocke le salaire reçu.
    }


    // ----- DATE D'EXPIRATION -----

    public LocalDate getDateExpiration() {
        // Permet de lire la date limite de l'offre.
        return dateExpiration;
        // Renvoie la date limite.
    }

    public void setDateExpiration(LocalDate dateExpiration) {
        // Permet de modifier la date limite.
        this.dateExpiration = dateExpiration;
        // Stocke la nouvelle date.
    }


    // ----- STATUT -----

    public OffreStatut getStatut() {
        // Permet de lire le statut de l'offre.
        return statut;
        // Renvoie le statut actuel.
    }

    public void setStatut(OffreStatut statut) {
        // Permet de changer le statut de l'offre.
        this.statut = statut;
        // Stocke le nouveau statut.
    }


    // ----- NOM DE L'ENTREPRISE -----

    public String getNomEntreprise() { return nomEntreprise; }
    // Renvoie le nom de l'entreprise.

    public void setNomEntreprise(String nomEntreprise) { this.nomEntreprise = nomEntreprise; }
    // Enregistre le nom de l'entreprise reçu.


    // ----- IDENTIFIANT ENTREPRISE -----

    public Long getEntrepriseId() {
        // Permet de lire l'identifiant de l'entreprise.
        return entrepriseId;
        // Renvoie l'identifiant.
    }

    public void setEntrepriseId(Long entrepriseId) {
        // Permet de renseigner l'entreprise.
        this.entrepriseId = entrepriseId;
        // Stocke l'identifiant reçu.
    }


    // ----- TEST TECHNIQUE -----

    public Long getTestRequisId() {
        // Permet de lire l'identifiant du test.
        return testRequisId;
        // Renvoie l'identifiant du test.
    }

    public void setTestRequisId(Long testRequisId) {
        // Permet d'associer un test technique.
        this.testRequisId = testRequisId;
        // Stocke l'identifiant du test.
    }


    // ----- RECRUTEUR -----

    public Long getRecruteurId() {
        // Permet de connaître le recruteur de l'offre.
        return recruteurId;
        // Renvoie son identifiant.
    }

    public void setRecruteurId(Long recruteurId) {
        // Permet d'indiquer le recruteur propriétaire.
        this.recruteurId = recruteurId;
        // Stocke son identifiant.
    }

} // Fin de la classe Offre.
