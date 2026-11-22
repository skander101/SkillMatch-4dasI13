package tn.esprit.skillmatch.offres.entity; // Place ce fichier dans le package indiqué, qui organise les classes du service.

import jakarta.validation.Validation; // Importe la création du moteur de validation.
import jakarta.validation.Validator; // Importe l'outil qui vérifie les annotations de validation.
import org.junit.jupiter.api.Test; // Importe l'annotation qui désigne un test JUnit.
import tn.esprit.skillmatch.offres.enums.ContratType; // Importe les valeurs autorisées de ContratType.

import java.time.LocalDate; // Importe les dates sans heure.
import java.util.Set; // Importe une collection sans doublons.
import java.util.stream.Collectors; // Importe la collecte des résultats d'un parcours de liste.

import static org.junit.jupiter.api.Assertions.*; // Importe les vérifications JUnit comme assertEquals et assertTrue.

class ValidationTest { // Regroupe les tests de ValidationTest.
    @Test // Indique que JUnit doit exécuter cette méthode comme un test.
    void valideUneOffreEtRefuseLesChampsInvalides() { // Compare une offre valide avec une offre contenant plusieurs erreurs.
        try (var factory = Validation.buildDefaultValidatorFactory()) { // Crée le moteur de validation et garantit sa fermeture à la fin du bloc.
            Validator validator = factory.getValidator(); // Récupère l'objet qui vérifie les contraintes sur les champs.
            Offre offre = new Offre(); // Crée une offre Java vide pour le test.
            offre.setTitre("Développeur Java"); // Renseigne un titre valide.
            offre.setContrat(ContratType.CDI); // Choisit le contrat CDI.
            offre.setDateExpiration(LocalDate.now().plusDays(30)); // Fixe la date d'expiration à trente jours après aujourd'hui.
            offre.setRecruteurId(1L); // Utilise un identifiant de recruteur de test, sans vérifier un autre service.
            offre.setSalaire(0.0); // Vérifie que le salaire zéro est autorisé.
            assertTrue(validator.validate(offre).isEmpty()); // Vérifie que cette offre ne présente aucune erreur de validation.

            offre.setTitre(" "); // Rend volontairement le titre invalide avec un simple espace.
            offre.setSalaire(-1.0); // Rend volontairement le salaire négatif.
            offre.setStatut(null); // Retire le statut pour provoquer une erreur de validation.
            offre.setDateExpiration(null); // Retire la date obligatoire.
            offre.setRecruteurId(0L); // Utilise zéro pour vérifier le refus d'un identifiant non positif.
            Set<String> erreurs = validator.validate(offre).stream() // Valide l'offre invalide et commence à parcourir les erreurs.
                    .map(erreur -> erreur.getPropertyPath().toString()).collect(Collectors.toSet()); // Récupère les noms des champs en erreur dans un ensemble.
            assertTrue(erreurs.containsAll( // Vérifie que tous les champs volontairement invalides ont été signalés.
                    Set.of("titre", "salaire", "statut", "dateExpiration", "recruteurId"))); // Liste les cinq champs qui doivent être en erreur.
        } // Ferme le bloc précédent (méthode, condition ou classe).
    } // Ferme le bloc précédent (méthode, condition ou classe).

    @Test // Indique que JUnit doit exécuter cette méthode comme un test.
    void candidatureExigeUneOffreEtUnCandidatMaisPasDeCv() { // Teste les références obligatoires d'une candidature.
        try (var factory = Validation.buildDefaultValidatorFactory()) { // Crée le moteur de validation et garantit sa fermeture à la fin du bloc.
            Candidature candidature = new Candidature(); // Crée une candidature Java vide pour le test.
            Set<String> erreurs = factory.getValidator().validate(candidature).stream() // Valide la candidature vide et parcourt les erreurs trouvées.
                    .map(erreur -> erreur.getPropertyPath().toString()).collect(Collectors.toSet()); // Récupère les noms des champs en erreur dans un ensemble.
            assertTrue(erreurs.containsAll(Set.of("offre", "userId"))); // L’offre et le candidat sont obligatoires.
            candidature.setOffre(new Offre()); // Fournit une offre Java ; ce test ne vérifie pas son existence en base.
            candidature.setUserId(2L); // Utilise un identifiant de candidat de test, sans vérification externe.
            assertFalse(erreurs.contains("cvId")); // Le CV est facultatif.
            candidature.setCvId(null); // Une candidature peut être enregistrée sans CV.
            assertTrue(factory.getValidator().validate(candidature).isEmpty()); // Vérifie les contraintes de la candidature une fois ses références renseignées.
        } // Ferme le bloc précédent (méthode, condition ou classe).
    } // Ferme le bloc précédent (méthode, condition ou classe).
} // Ferme le bloc précédent (méthode, condition ou classe).
