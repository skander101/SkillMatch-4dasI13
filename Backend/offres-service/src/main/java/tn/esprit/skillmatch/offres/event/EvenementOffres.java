package tn.esprit.skillmatch.offres.event;

import tn.esprit.skillmatch.offres.entity.*;
import tn.esprit.skillmatch.offres.enums.CandidatureStatut;
import java.time.*;
import java.util.*;

// Une enveloppe commune ; seules les données utiles sont envoyées, jamais le JWT ou la lettre.
public record EvenementOffres(UUID eventId, String type, Instant date, Object donnees) {
    public static EvenementOffres offrePubliee(Offre o) {
        return creer("OfferPublished", new OffrePubliee(o.getId(), o.getRecruteurId(), o.getTitre(),
                List.copyOf(o.getCompetences()), o.getLocalisation(), o.getContrat().name(), o.getSalaire(), o.getDateExpiration()));
    }

    public static EvenementOffres candidatureDeposee(Candidature c) {
        return creer("ApplicationSubmitted", new CandidatureDeposee(c.getId(), c.getOffre().getId(),
                c.getUserId(), c.getOffre().getRecruteurId(), c.getOffre().getTitre()));
    }

    public static EvenementOffres statutChange(Candidature c, CandidatureStatut ancien) {
        return creer("StatusChanged", new StatutChange(c.getId(), c.getOffre().getId(), c.getUserId(),
                c.getOffre().getRecruteurId(), ancien.name(), c.getStatut().name(), c.getDateEntretien()));
    }

    private static EvenementOffres creer(String type, Object donnees) {
        return new EvenementOffres(UUID.randomUUID(), type, Instant.now(), donnees);
    }

    public record OffrePubliee(Long offreId, Long recruteurId, String titre, List<String> competences,
                              String localisation, String contrat, Double salaire, LocalDate dateExpiration) {}

    public record CandidatureDeposee(Long candidatureId, Long offreId, Long candidatId, Long recruteurId, String titreOffre) {}

    public record StatutChange(Long candidatureId, Long offreId, Long candidatId, Long recruteurId,
                               String ancienStatut, String nouveauStatut, LocalDateTime dateEntretien) {}
}
