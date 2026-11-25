
package tn.esprit.skillmatch.offres.dto;

import jakarta.validation.constraints.*;
import tn.esprit.skillmatch.offres.enums.CandidatureStatut;
import java.time.LocalDate;
import java.time.LocalDateTime;


// ========== FORMULAIRE DE MODIFICATION ==========

// Contient les informations nécessaires pour modifier une candidature.
// Les champs absents conservent leur valeur actuelle grâce au service.
public record CandidatureUpdate(


    // ========== LETTRE DE MOTIVATION ==========

    // Lettre modifiable par le candidat et ADMIN.
    // Refuse un texte vide et impose 20 à 10000 caractères.
    @Pattern(regexp = "(?s).*\\S.*") @Size(min = 20, max = 10000) String lettre,


    // ========== IDENTIFIANT DU CV ==========

    // CV facultatif, modifiable par le candidat et ADMIN.
    // L'identifiant doit être positif s'il est renseigné.
    @Positive Long cvId,


    // ========== STATUT DE LA CANDIDATURE ==========

    // Nouveau statut de la candidature.
    // Modifiable par le recruteur ou ADMIN selon les droits.
    CandidatureStatut statut,


    // ========== DATE D'ENTRETIEN ==========

    // Date et heure de l'entretien.
    // La date doit être aujourd'hui ou dans le futur.
    LocalDateTime dateEntretien,


    // ========== NOTE DU RECRUTEUR ==========

    // Note donnée par le recruteur.
    // Accepte zéro ou un nombre positif.
    @PositiveOrZero Integer noteRecruteur,


    // ========== COMMENTAIRE DU RECRUTEUR ==========

    // Commentaire du recruteur.
    // Refuse un texte vide et impose 5 à 10000 caractères.
    @Pattern(regexp = "(?s).*\\S.*") @Size(min = 5, max = 10000) String commentaireRecruteur

) {


    // ========== NETTOYAGE DES TEXTES ==========

    // Constructeur qui nettoie les textes reçus.
    public CandidatureUpdate {

        // Supprime les espaces autour de la lettre si elle existe.
        lettre = lettre == null ? null : lettre.trim();

        // Supprime les espaces autour du commentaire s'il existe.
        commentaireRecruteur = commentaireRecruteur == null ? null : commentaireRecruteur.trim();
    }


    // ========== VALIDATION DE LA DATE D'ENTRETIEN ==========

    // Vérifie que la date est aujourd'hui ou dans le futur.
    // Affiche un message d'erreur si la vérification échoue.
    @AssertTrue(message = "La date d'entretien doit être aujourd'hui ou dans le futur")

    // Méthode qui vérifie si la date d'entretien est valide.
    public boolean isDateEntretienValide() {

        // Retourne true si la date est absente ou non passée.
        // Retourne false si la date est avant aujourd'hui.
        return dateEntretien == null || !dateEntretien.toLocalDate().isBefore(LocalDate.now());
    }
}
