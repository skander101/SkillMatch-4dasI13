package tn.esprit.skillmatch.offres.entity;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import tn.esprit.skillmatch.offres.enums.ContratType;

import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class ValidationTest {
    @Test
    void valideUneOffreEtRefuseLesChampsInvalides() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            Offre offre = new Offre();
            offre.setTitre("Développeur Java");
            offre.setContrat(ContratType.CDI);
            offre.setDateExpiration(LocalDate.now().plusDays(30));
            offre.setRecruteurId(1L);
            offre.setSalaire(0.0);
            assertTrue(validator.validate(offre).isEmpty());

            offre.setTitre(" ");
            offre.setSalaire(-1.0);
            offre.setStatut(null);
            offre.setDateExpiration(null);
            offre.setRecruteurId(0L);
            Set<String> erreurs = validator.validate(offre).stream()
                    .map(erreur -> erreur.getPropertyPath().toString()).collect(Collectors.toSet());
            assertTrue(erreurs.containsAll(
                    Set.of("titre", "salaire", "statut", "dateExpiration", "recruteurId")));
        }
    }

    @Test
    void candidatureExigeUneOffreUnCandidatEtUnCv() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            Candidature candidature = new Candidature();
            Set<String> erreurs = factory.getValidator().validate(candidature).stream()
                    .map(erreur -> erreur.getPropertyPath().toString()).collect(Collectors.toSet());
            assertTrue(erreurs.containsAll(Set.of("offre", "userId", "cvId")));
            candidature.setOffre(new Offre());
            candidature.setUserId(2L);
            candidature.setCvId(3L);
            assertTrue(factory.getValidator().validate(candidature).isEmpty());
        }
    }
}
