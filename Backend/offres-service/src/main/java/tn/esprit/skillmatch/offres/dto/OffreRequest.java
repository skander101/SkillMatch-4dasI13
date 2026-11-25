
package tn.esprit.skillmatch.offres.dto;

import jakarta.validation.constraints.*;
import tn.esprit.skillmatch.offres.enums.ContratType;
import tn.esprit.skillmatch.offres.enums.OffreStatut;
import java.time.LocalDate;
import java.util.List;


// ========== FORMULAIRE DE CRÉATION D'OFFRE ==========

// Contient les informations nécessaires pour créer une offre d'emploi.
public record OffreRequest(


    // ========== TITRE ==========

    // Titre obligatoire, entre 3 et 255 caractères.
    @NotBlank @Size(min = 3, max = 255) String titre,


    // ========== DESCRIPTION ==========

    // Description obligatoire, entre 20 et 10000 caractères.
    @NotBlank @Size(min = 20, max = 10000) String description,


    // ========== COMPÉTENCES ==========

    // Liste contenant au moins une compétence.
    // Chaque compétence doit être non vide et limitée à 255 caractères.
    @NotEmpty List<@NotBlank @Size(max = 255) String> competences,


    // ========== LOCALISATION ==========

    // Lieu de travail obligatoire, limité à 255 caractères.
    @NotBlank @Size(max = 255) String localisation,


    // ========== TYPE DE CONTRAT ==========

    // Type de contrat obligatoire (CDI, CDD, STAGE...).
    @NotNull ContratType contrat,


    // ========== SALAIRE ==========

    // Salaire obligatoire, supérieur ou égal à zéro.
    @NotNull @PositiveOrZero Double salaire,


    // ========== DATE D'EXPIRATION ==========

    // Date obligatoire, aujourd'hui ou dans le futur.
    @NotNull @FutureOrPresent LocalDate dateExpiration,


    // ========== STATUT DE L'OFFRE ==========

    // Statut obligatoire ; à la création, le service exige BROUILLON.
    @NotNull OffreStatut statut,


    // ========== IDENTIFIANT ENTREPRISE ==========

    // Identifiant facultatif de l'entreprise, positif si renseigné.
    @Positive Long entrepriseId,


    // ========== TEST TECHNIQUE ==========

    // Identifiant facultatif du test technique, positif si renseigné.
    @Positive Long testRequisId,


    // ========== IDENTIFIANT DU RECRUTEUR ==========

    // Identifiant du recruteur, obligatoire pour ADMIN selon le service.
    // Doit être positif s'il est renseigné.
    @Positive Long recruteurId,


    // ========== NOM DE L'ENTREPRISE ==========

    // Nom obligatoire, entre 2 et 255 caractères.
    @NotBlank @Size(min = 2, max = 255) String nomEntreprise

) {


    // ========== NETTOYAGE DES TEXTES ==========

    // Constructeur qui nettoie les textes du formulaire.
    public OffreRequest {

        // Supprime les espaces autour du nom de l'entreprise.
        nomEntreprise = nomEntreprise == null ? null : nomEntreprise.trim();

        // Supprime les espaces autour du titre.
        titre = titre == null ? null : titre.trim();

        // Supprime les espaces autour de la description.
        description = description == null ? null : description.trim();

        // Supprime les espaces autour de la localisation.
        localisation = localisation == null ? null : localisation.trim();
    }

    // L'identifiant de l'offre et le rôle ne sont pas définis par le formulaire.
}
