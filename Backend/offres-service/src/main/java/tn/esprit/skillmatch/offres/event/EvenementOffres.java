
package tn.esprit.skillmatch.offres.event;

import tn.esprit.skillmatch.offres.entity.*;
import tn.esprit.skillmatch.offres.enums.CandidatureStatut;
import java.time.*;
import java.util.*;


// ========== CLASSE DES ÉVÉNEMENTS ==========

// Représente un événement contenant les informations utiles
// à transmettre aux autres microservices.
public record EvenementOffres(UUID eventId, String type, Instant date, Object donnees) {


    // ========== 1. OFFRE PUBLIÉE ==========

    // Prépare un événement lorsqu'une offre est publiée.
    public static EvenementOffres offrePubliee(Offre o) {

        // Crée l'événement OfferPublished avec les informations de l'offre.
        return creer("OfferPublished", new OffrePubliee(o.getId(), o.getRecruteurId(), o.getTitre(),

            // Ajoute les compétences et les autres informations de l'offre.
            List.copyOf(o.getCompetences()), o.getLocalisation(), o.getContrat().name(), o.getSalaire(), o.getDateExpiration()));
    }


    // ========== 2. CANDIDATURE DÉPOSÉE ==========

    // Prépare un événement lorsqu'un candidat postule à une offre.
    public static EvenementOffres candidatureDeposee(Candidature c) {

        // Crée l'événement ApplicationSubmitted avec les identifiants concernés.
        return creer("ApplicationSubmitted", new CandidatureDeposee(c.getId(), c.getOffre().getId(),

            // Ajoute les informations du candidat, du recruteur et de l'offre.
            c.getUserId(), c.getOffre().getRecruteurId(), c.getOffre().getTitre()));
    }


    // ========== 3. CHANGEMENT DE STATUT ==========

    // Prépare un événement lorsque le statut d'une candidature change.
    public static EvenementOffres statutChange(Candidature c, CandidatureStatut ancien) {

        // Crée l'événement StatusChanged avec les informations de la candidature.
        return creer("StatusChanged", new StatutChange(c.getId(), c.getOffre().getId(), c.getUserId(),

            // Ajoute l'ancien statut, le nouveau statut et la date d'entretien.
            c.getOffre().getRecruteurId(), ancien.name(), c.getStatut().name(), c.getDateEntretien()));
    }


    // ========== 4. CRÉER UN ÉVÉNEMENT ==========

    // Méthode commune qui permet de créer les différents événements.
    private static EvenementOffres creer(String type, Object donnees) {

        // Crée un événement avec un identifiant unique et la date actuelle.
        return new EvenementOffres(UUID.randomUUID(), type, Instant.now(), donnees);
    }


    // ========== 5. DONNÉES D'UNE OFFRE PUBLIÉE ==========

    // Définit les informations à transmettre lorsqu'une offre est publiée.
    public record OffrePubliee(Long offreId, Long recruteurId, String titre, List<String> competences,
                               String localisation, String contrat, Double salaire, LocalDate dateExpiration) {}


    // ========== 6. DONNÉES D'UNE CANDIDATURE DÉPOSÉE ==========

    // Définit les informations à transmettre lorsqu'un candidat postule.
    public record CandidatureDeposee(Long candidatureId, Long offreId, Long candidatId, Long recruteurId, String titreOffre) {}


    // ========== 7. DONNÉES D'UN CHANGEMENT DE STATUT ==========

    // Définit les informations à transmettre lorsqu'une candidature change de statut.
    public record StatutChange(Long candidatureId, Long offreId, Long candidatId, Long recruteurId,
                               String ancienStatut, String nouveauStatut, LocalDateTime dateEntretien) {}
}
