package tn.esprit.skillmatch.offres.entity; // Place ce fichier dans le package indiqué, qui organise les classes du service.

import jakarta.persistence.*; // Importe les annotations JPA pour les tables, colonnes et relations.
import jakarta.validation.constraints.*; // Importe les annotations de validation des champs.
import tn.esprit.skillmatch.offres.enums.ContratType; // Importe les valeurs autorisées de ContratType.
import tn.esprit.skillmatch.offres.enums.OffreStatut; // Importe les valeurs autorisées de OffreStatut.

import java.time.LocalDate; // Importe les dates sans heure.
import java.util.ArrayList; // Importe une liste modifiable.
import java.util.List; // Importe le type liste.
import java.util.Locale; // Importe une règle stable pour convertir les textes en minuscules.
import java.util.Set; // Importe une collection sans doublons.
import java.util.stream.Collectors; // Importe la collecte des résultats d'un parcours de liste.

@Entity // Indique à JPA que cette classe est enregistrée dans une table.
@Table(name = "offres") // Associe cette classe à la table offres dans offres_db.
public class Offre { // Déclare l'entité Offre, avec ses champs et ses méthodes.
    @Id // Désigne la clé primaire de la table.
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Laisse MySQL générer l'identifiant avec AUTO_INCREMENT.
    private Long id; // Déclare l'identifiant.

    @NotBlank // Refuse un texte null, vide ou composé seulement d'espaces.
    @Size(max = 255) // Limite le texte à 255 caractères.
    @Column(nullable = false) // Interdit NULL dans cette colonne de la base.
    private String titre; // Déclare le titre.

    @Column(columnDefinition = "TEXT") // Utilise une colonne TEXT pour stocker un texte long.
    private String description; // Déclare la description.

    @NotNull // Refuse une valeur null lors de la validation.
    @Convert(converter = CompetencesConverter.class) // Demande à JPA d'utiliser notre convertisseur pour ce champ.
    @Column(columnDefinition = "TEXT", nullable = false) // Stocke le JSON dans une colonne TEXT qui refuse NULL.
    private List<@NotBlank String> competences = new ArrayList<>(); // Déclare la liste des compétences ; chaque texte doit être non vide et la liste commence vide.

    @Size(max = 255) // Limite le texte à 255 caractères.
    private String localisation; // Déclare la localisation.

    @NotNull // Refuse une valeur null lors de la validation.
    @Enumerated(EnumType.STRING) // Enregistre le nom du statut ou du contrat en base, plutôt que son numéro.
    @Column(nullable = false) // Interdit NULL dans cette colonne de la base.
    private ContratType contrat; // Déclare le type de contrat.

    @PositiveOrZero // Autorise zéro ou un salaire positif ; un salaire absent reste autorisé.
    private Double salaire; // Déclare le salaire.

    @NotNull // Refuse une valeur null lors de la validation.
    @Column(nullable = false) // Interdit NULL dans cette colonne de la base.
    private LocalDate dateExpiration; // Déclare la date d'expiration.

    @NotNull // Refuse une valeur null lors de la validation.
    @Enumerated(EnumType.STRING) // Enregistre le nom du statut ou du contrat en base, plutôt que son numéro.
    @Column(nullable = false) // Interdit NULL dans cette colonne de la base.
    private OffreStatut statut = OffreStatut.BROUILLON; // Déclare le statut ; BROUILLON est la valeur initiale.

    @Positive // Exige un identifiant strictement supérieur à zéro s'il est renseigné.
    private Long entrepriseId; // Déclare l'identifiant de l'entreprise.

    @Positive // Exige un identifiant strictement supérieur à zéro s'il est renseigné.
    private Long testRequisId; // Déclare l'identifiant du test requis.

    // Le proprietaire est un utilisateur, pas une entreprise.
    @NotNull // Refuse une valeur null lors de la validation.
    @Positive // Exige un identifiant strictement supérieur à zéro s'il est renseigné.
    @Column(nullable = false) // Interdit NULL dans cette colonne de la base.
    private Long recruteurId; // Déclare l'identifiant du recruteur propriétaire.

    public Boolean estExpiree() { // Vérifie si la date limite de l'offre est passée.
        return dateExpiration != null && dateExpiration.isBefore(LocalDate.now()); // Retourne vrai seulement si la date existe et est avant aujourd'hui.
    } // Ferme le bloc précédent (méthode, condition ou classe).

    // Pourcentage de competences requises presentes, sans recommandations.
    public Double calculerScore(List<String> competencesProfil) { // Calcule le pourcentage des compétences de l'offre présentes dans le profil fourni.
        if (competences == null || competences.isEmpty() || competencesProfil == null) { // Traite le cas où les compétences nécessaires au calcul sont absentes.
            return 0.0; // Retourne un score nul quand la comparaison n'est pas possible.
        } // Ferme le bloc précédent (méthode, condition ou classe).
        Set<String> requises = normaliser(competences); // Nettoie les compétences de l'offre et retire les doublons.
        if (requises.isEmpty()) { // Évite de diviser par zéro si aucune compétence valide n'est requise.
            return 0.0; // Retourne un score nul quand la comparaison n'est pas possible.
        } // Ferme le bloc précédent (méthode, condition ou classe).
        Set<String> profil = normaliser(competencesProfil); // Nettoie aussi les compétences du profil.
        long communes = requises.stream().filter(profil::contains).count(); // Compte les compétences requises qui existent dans le profil.
        return 100.0 * communes / requises.size(); // Transforme le nombre de compétences communes en pourcentage.
    } // Ferme le bloc précédent (méthode, condition ou classe).

    private Set<String> normaliser(List<String> valeurs) { // Prépare des textes comparables et sans doublons.
        return valeurs.stream() // Commence à parcourir les valeurs de la liste.
                .filter(valeur -> valeur != null && !valeur.isBlank()) // Ignore les valeurs nulles et les textes vides.
                .map(valeur -> valeur.trim().toLowerCase(Locale.ROOT)) // Retire les espaces aux extrémités et passe en minuscules.
                .collect(Collectors.toSet()); // Rassemble les textes dans un ensemble qui supprime les doublons.
    } // Ferme le bloc précédent (méthode, condition ou classe).

    public Long getId() { // Permet de lire l'identifiant.
        return id; // Retourne l'identifiant de cet objet.
    } // Ferme le bloc précédent (méthode, condition ou classe).

    public void setId(Long id) { // Permet de renseigner ou modifier l'identifiant.
        this.id = id; // Stocke la valeur reçue dans le champ de cet objet.
    } // Ferme le bloc précédent (méthode, condition ou classe).

    public String getTitre() { // Permet de lire le titre.
        return titre; // Retourne le titre de cet objet.
    } // Ferme le bloc précédent (méthode, condition ou classe).

    public void setTitre(String titre) { // Permet de renseigner ou modifier le titre.
        this.titre = titre; // Stocke la valeur reçue dans le champ de cet objet.
    } // Ferme le bloc précédent (méthode, condition ou classe).

    public String getDescription() { // Permet de lire la description.
        return description; // Retourne la description de cet objet.
    } // Ferme le bloc précédent (méthode, condition ou classe).

    public void setDescription(String description) { // Permet de renseigner ou modifier la description.
        this.description = description; // Stocke la valeur reçue dans le champ de cet objet.
    } // Ferme le bloc précédent (méthode, condition ou classe).

    public List<String> getCompetences() { // Permet de lire la liste des compétences.
        return competences; // Retourne la liste des compétences de cet objet.
    } // Ferme le bloc précédent (méthode, condition ou classe).

    public void setCompetences(List<String> competences) { // Permet de renseigner ou modifier la liste des compétences.
        this.competences = competences; // Stocke la valeur reçue dans le champ de cet objet.
    } // Ferme le bloc précédent (méthode, condition ou classe).

    public String getLocalisation() { // Permet de lire la localisation.
        return localisation; // Retourne la localisation de cet objet.
    } // Ferme le bloc précédent (méthode, condition ou classe).

    public void setLocalisation(String localisation) { // Permet de renseigner ou modifier la localisation.
        this.localisation = localisation; // Stocke la valeur reçue dans le champ de cet objet.
    } // Ferme le bloc précédent (méthode, condition ou classe).

    public ContratType getContrat() { // Permet de lire le type de contrat.
        return contrat; // Retourne le type de contrat de cet objet.
    } // Ferme le bloc précédent (méthode, condition ou classe).

    public void setContrat(ContratType contrat) { // Permet de renseigner ou modifier le type de contrat.
        this.contrat = contrat; // Stocke la valeur reçue dans le champ de cet objet.
    } // Ferme le bloc précédent (méthode, condition ou classe).

    public Double getSalaire() { // Permet de lire le salaire.
        return salaire; // Retourne le salaire de cet objet.
    } // Ferme le bloc précédent (méthode, condition ou classe).

    public void setSalaire(Double salaire) { // Permet de renseigner ou modifier le salaire.
        this.salaire = salaire; // Stocke la valeur reçue dans le champ de cet objet.
    } // Ferme le bloc précédent (méthode, condition ou classe).

    public LocalDate getDateExpiration() { // Permet de lire la date d'expiration.
        return dateExpiration; // Retourne la date d'expiration de cet objet.
    } // Ferme le bloc précédent (méthode, condition ou classe).

    public void setDateExpiration(LocalDate dateExpiration) { // Permet de renseigner ou modifier la date d'expiration.
        this.dateExpiration = dateExpiration; // Stocke la valeur reçue dans le champ de cet objet.
    } // Ferme le bloc précédent (méthode, condition ou classe).

    public OffreStatut getStatut() { // Permet de lire le statut.
        return statut; // Retourne le statut de cet objet.
    } // Ferme le bloc précédent (méthode, condition ou classe).

    public void setStatut(OffreStatut statut) { // Permet de renseigner ou modifier le statut.
        this.statut = statut; // Stocke la valeur reçue dans le champ de cet objet.
    } // Ferme le bloc précédent (méthode, condition ou classe).

    public Long getEntrepriseId() { // Permet de lire l'identifiant de l'entreprise.
        return entrepriseId; // Retourne l'identifiant de l'entreprise de cet objet.
    } // Ferme le bloc précédent (méthode, condition ou classe).

    public void setEntrepriseId(Long entrepriseId) { // Permet de renseigner ou modifier l'identifiant de l'entreprise.
        this.entrepriseId = entrepriseId; // Stocke la valeur reçue dans le champ de cet objet.
    } // Ferme le bloc précédent (méthode, condition ou classe).

    public Long getTestRequisId() { // Permet de lire l'identifiant du test requis.
        return testRequisId; // Retourne l'identifiant du test requis de cet objet.
    } // Ferme le bloc précédent (méthode, condition ou classe).

    public void setTestRequisId(Long testRequisId) { // Permet de renseigner ou modifier l'identifiant du test requis.
        this.testRequisId = testRequisId; // Stocke la valeur reçue dans le champ de cet objet.
    } // Ferme le bloc précédent (méthode, condition ou classe).

    public Long getRecruteurId() { // Permet de lire l'identifiant du recruteur propriétaire.
        return recruteurId; // Retourne l'identifiant du recruteur propriétaire de cet objet.
    } // Ferme le bloc précédent (méthode, condition ou classe).

    public void setRecruteurId(Long recruteurId) { // Permet de renseigner ou modifier l'identifiant du recruteur propriétaire.
        this.recruteurId = recruteurId; // Stocke la valeur reçue dans le champ de cet objet.
    } // Ferme le bloc précédent (méthode, condition ou classe).
} // Ferme le bloc précédent (méthode, condition ou classe).
