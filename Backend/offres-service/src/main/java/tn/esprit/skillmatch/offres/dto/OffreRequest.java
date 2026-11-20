package tn.esprit.skillmatch.offres.dto; // Données acceptées dans le formulaire HTTP.

import jakarta.validation.constraints.*; // Validations du formulaire.
import tn.esprit.skillmatch.offres.enums.ContratType; // Contrats autorisés.
import tn.esprit.skillmatch.offres.enums.OffreStatut; // Statuts autorisés.
import java.time.LocalDate; // Date sans heure.
import java.util.List; // Liste des compétences.

public record OffreRequest( // Un record évite d'écrire des getters et setters de formulaire.
        @NotBlank @Size(max = 255) String titre, // Titre obligatoire.
        String description, // Description libre.
        @NotNull List<@NotBlank String> competences, // Liste obligatoire ; peut être vide.
        @Size(max = 255) String localisation, // Localisation limitée à la taille de la colonne.
        @NotNull ContratType contrat, // Contrat obligatoire.
        @PositiveOrZero Double salaire, // Salaire absent ou positif ou nul.
        @NotNull LocalDate dateExpiration, // La cohérence de la date est vérifiée par le service.
        @NotNull OffreStatut statut, // La création exige BROUILLON.
        @Positive Long entrepriseId, // Référence simple, sans appel Entreprise.
        @Positive Long testRequisId, // Référence simple, sans appel Tests.
        @Positive Long recruteurId // Obligatoire pour ADMIN à la création ; jamais une preuve d'identité.
) { // Aucun id d'offre ni rôle ne peut être affecté par le formulaire.
} // Fin du DTO.
