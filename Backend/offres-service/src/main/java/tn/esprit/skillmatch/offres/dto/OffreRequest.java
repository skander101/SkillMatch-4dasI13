package tn.esprit.skillmatch.offres.dto; // Données acceptées dans le formulaire HTTP.

import jakarta.validation.constraints.*; // Validations du formulaire.
import tn.esprit.skillmatch.offres.enums.ContratType; // Contrats autorisés.
import tn.esprit.skillmatch.offres.enums.OffreStatut; // Statuts autorisés.
import java.time.LocalDate; // Date sans heure.
import java.util.List; // Liste des compétences.

public record OffreRequest( // Un record évite d'écrire des getters et setters de formulaire.
        @NotBlank @Size(min = 3, max = 255) String titre, // Titre obligatoire.
        @NotBlank @Size(min = 20, max = 10000) String description, // Même limite que le formulaire.
        @NotEmpty List<@NotBlank @Size(max = 255) String> competences, // Au moins une compétence.
        @NotBlank @Size(max = 255) String localisation, // Localisation limitée à la taille de la colonne.
        @NotNull ContratType contrat, // Contrat obligatoire.
        @NotNull @PositiveOrZero Double salaire, // Salaire obligatoire, positif ou nul.
        @NotNull @FutureOrPresent LocalDate dateExpiration, // La cohérence de la date est vérifiée par le service.
        @NotNull OffreStatut statut, // La création exige BROUILLON.
        @NotNull @Positive Long entrepriseId, // Référence simple, sans appel Entreprise.
        @NotNull @Positive Long testRequisId, // Référence simple, sans appel Tests.
        @Positive Long recruteurId // Obligatoire pour ADMIN à la création ; jamais une preuve d'identité.
) {
    public OffreRequest { // Nettoie les espaces avant de vérifier les longueurs.
        titre = titre == null ? null : titre.trim();
        description = description == null ? null : description.trim();
        localisation = localisation == null ? null : localisation.trim();
    }
    // Aucun id d'offre ni rôle ne peut être affecté par le formulaire.
} // Fin du DTO.
