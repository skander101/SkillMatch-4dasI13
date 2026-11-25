
package tn.esprit.skillmatch.offres.dto;

import jakarta.validation.constraints.*;


// ========== FORMULAIRE DE CANDIDATURE ==========

// Contient les informations nécessaires pour postuler à une offre.
public record CandidatureRequest(


    // ========== IDENTIFIANT DE L'OFFRE ==========

    // Identifiant obligatoire de l'offre choisie.
    // Doit être supérieur à zéro.
    @NotNull @Positive Long offreId,


    // ========== LETTRE DE MOTIVATION ==========

    // Lettre obligatoire, non vide.
    // Doit contenir entre 20 et 10000 caractères.
    @NotBlank @Size(min = 20, max = 10000) String lettre,


    // ========== IDENTIFIANT DU CV ==========

    // Identifiant du CV, facultatif pour postuler.
    // Doit être positif s'il est renseigné.
    @Positive Long cvId,


    // ========== IDENTIFIANT DU CANDIDAT ==========

    // Identifiant du candidat, réservé à ADMIN.
    // Le candidat normal utilise sa session.
    // Doit être positif s'il est renseigné.
    @Positive Long userId

) {


    // ========== NETTOYAGE DE LA LETTRE ==========

    // Constructeur qui nettoie la lettre de motivation.
    public CandidatureRequest {

        // Supprime les espaces autour de la lettre si elle existe.
        // Si elle est null, conserve null.
        lettre = lettre == null ? null : lettre.trim();
    }

}
