package tn.esprit.skillmatch.offres.service; // Règles essentielles des candidatures.

import org.springframework.http.HttpStatus; // Codes des erreurs.
import org.springframework.stereotype.Service; // Service Spring.
import org.springframework.transaction.annotation.Transactional; // Transactions.
import org.springframework.web.server.ResponseStatusException; // Erreurs HTTP.
import tn.esprit.skillmatch.offres.dto.*; // Formulaires et réponse.
import tn.esprit.skillmatch.offres.entity.*; // Offre et candidature.
import tn.esprit.skillmatch.offres.enums.*; // Statuts autorisés.
import tn.esprit.skillmatch.offres.repository.*; // Accès MySQL.
import tn.esprit.skillmatch.offres.security.IdentiteService; // Authentification existante.
import tn.esprit.skillmatch.offres.security.IdentiteService.Identite; // Utilisateur vérifié.
import java.time.LocalDateTime; // Vérification de l'entretien.
import java.util.List; // Résultats.
import java.util.Set; // Statuts accessibles au recruteur.
import java.util.Comparator; // Compare les dates pour le tri.
import java.util.Locale; // Recherche sans tenir compte de la casse.

@Service
@Transactional // Annule l'opération si une règle échoue.
public class CandidatureService {
    private final CandidatureRepository candidatures;
    private final OffreRepository offres;
    private final IdentiteService identites;

    public CandidatureService(CandidatureRepository candidatures, OffreRepository offres, IdentiteService identites) { // Injection.
        this.candidatures = candidatures;
        this.offres = offres;
        this.identites = identites;
    }

    @Transactional(readOnly = true)
    public List<CandidatureResponse> lister(String token, String search, String sort) { // Deux paramètres simples, comme les offres.
        Identite u = identites.courante(token);
        List<Candidature> liste = switch (u.role()) {
            case "ADMIN" -> candidatures.findAll(); // Toutes.
            case "RECRUTEUR" -> candidatures.findByOffreRecruteurId(u.id()); // Candidatures de ses offres.
            default -> candidatures.findByUserId(u.id()); // Candidatures du candidat connecté.
        };
        return rechercherEtTrier(liste, search, sort); // Recherche seulement dans le périmètre autorisé.
    }

    @Transactional(readOnly = true)
    public CandidatureResponse consulter(Long id, String token) {
        return CandidatureResponse.depuis(trouverAccessible(id, identites.courante(token))); // Identité et propriété.
    }

    public CandidatureResponse creer(CandidatureRequest d, String token) {
        Identite u = identites.courante(token);
        if ("RECRUTEUR".equals(u.role())) throw erreur(403, "Un recruteur ne peut pas postuler pour un candidat");
        Long candidat = u.id(); // Identité de session par défaut.
        if ("ADMIN".equals(u.role())) {
            identites.verifierCandidat(d.userId(), token); // Utilisateur existant et rôle CANDIDAT.
            candidat = d.userId();
        } else if (d.userId() != null && !candidat.equals(d.userId())) {
            throw erreur(403, "Impossible de postuler au nom d'un autre candidat");
        }
        Offre offre = offres.findById(d.offreId()).orElseThrow(() -> erreur(404, "Offre introuvable"));
        if (offre.getStatut() != OffreStatut.PUBLIEE || offre.estExpiree()) throw erreur(409, "Cette offre n'est pas ouverte");
        if (candidatures.existsByUserIdAndOffreId(candidat, offre.getId())) throw erreur(409, "Vous avez déjà postulé à cette offre");
        Candidature c = new Candidature(); // Date et statut sont initialisés par @PrePersist.
        c.setOffre(offre);
        c.setUserId(candidat);
        c.setLettre(d.lettre());
        c.setCvId(d.cvId());
        return CandidatureResponse.depuis(candidatures.saveAndFlush(c)); // Détecte aussi les doublons simultanés.
    }

    public CandidatureResponse modifier(Long id, CandidatureUpdate d, String token) {
        Identite u = identites.courante(token);
        Candidature c = trouverAccessible(id, u);
        verifierModification(c, d, u); // Vérifie les champs accessibles au rôle.
        if (d.lettre() != null) c.setLettre(d.lettre()); // Un champ absent est conservé.
        if (d.cvId() != null) c.setCvId(d.cvId());
        if (d.statut() != null) c.changerStatut(d.statut());
        if (d.dateEntretien() != null) c.setDateEntretien(d.dateEntretien());
        if (d.noteRecruteur() != null) c.setNoteRecruteur(d.noteRecruteur());
        if (d.commentaireRecruteur() != null) c.setCommentaireRecruteur(d.commentaireRecruteur());
        return CandidatureResponse.depuis(candidatures.save(c));
    }

    public void supprimer(Long id, String token) {
        Identite u = identites.courante(token);
        Candidature c = trouverAccessible(id, u);
        if ("RECRUTEUR".equals(u.role())) throw erreur(403, "Le recruteur ne peut pas supprimer une candidature");
        if ("ADMIN".equals(u.role())) candidatures.delete(c); // Suppression physique, sans toucher à l'offre.
        else {
            if (c.getStatut() != CandidatureStatut.RECUE) throw erreur(409, "Seule une candidature RECUE peut être retirée");
            c.changerStatut(CandidatureStatut.RETIREE); // Retrait logique du candidat.
            candidatures.save(c);
        }
    }

    private Candidature trouverAccessible(Long id, Identite u) { // Même contrôle pour lire, modifier et supprimer.
        Candidature c = candidatures.findById(id).orElseThrow(() -> erreur(404, "Candidature introuvable"));
        boolean autorise = "ADMIN".equals(u.role()) || ("CANDIDAT".equals(u.role()) && c.getUserId().equals(u.id()))
                || ("RECRUTEUR".equals(u.role()) && c.getOffre().getRecruteurId().equals(u.id()));
        if (!autorise) throw erreur(403, "Cette candidature ne vous est pas accessible");
        return c;
    }

    private void verifierModification(Candidature c, CandidatureUpdate d, Identite u) {
        if ("CANDIDAT".equals(u.role())) {
            if (c.getStatut() != CandidatureStatut.RECUE) throw erreur(409, "Modification possible uniquement au statut RECUE");
            if (d.statut() != null || d.dateEntretien() != null || d.noteRecruteur() != null || d.commentaireRecruteur() != null)
                throw erreur(403, "Le candidat peut modifier seulement sa lettre et son cvId");
        }
        if ("RECRUTEUR".equals(u.role())) {
            if (c.getStatut() == CandidatureStatut.RETIREE) throw erreur(409, "Cette candidature a été retirée");
            if (d.lettre() != null || d.cvId() != null) throw erreur(403, "La lettre et le CV appartiennent au candidat");
            if (d.statut() != null && !Set.of(CandidatureStatut.PRESELECTIONNEE, CandidatureStatut.ENTRETIEN_PLANIFIE,
                    CandidatureStatut.ACCEPTEE, CandidatureStatut.REFUSEE).contains(d.statut())) throw erreur(403, "Statut réservé au candidat ou à ADMIN");
        }
        if (d.lettre() != null && d.lettre().isBlank()) throw erreur(400, "La lettre ne peut pas être vide");
        LocalDateTime entretien = d.dateEntretien() == null ? c.getDateEntretien() : d.dateEntretien(); // Date effective.
        if (d.statut() == CandidatureStatut.ENTRETIEN_PLANIFIE && (entretien == null || !entretien.isAfter(LocalDateTime.now())))
            throw erreur(400, "Planifier un entretien exige une date future");
    }

    private List<CandidatureResponse> rechercherEtTrier(List<Candidature> liste, String search, String sort) {
        String mot = search.trim().toLowerCase(Locale.ROOT); // Ignore casse et espaces autour du mot.
        Comparator<Candidature> ordre = switch (sort) {
            case "dateCandidature" -> Comparator.comparing(Candidature::getDateCandidature); // Anciennes d'abord.
            case "dateCandidatureDesc" -> Comparator.comparing(Candidature::getDateCandidature).reversed(); // Récentes d'abord.
            default -> throw erreur(400, "Tri : dateCandidature ou dateCandidatureDesc"); // Refuse un tri inconnu.
        };
        return liste.stream()
                .filter(c -> mot.isEmpty() || (c.getLettre() != null && c.getLettre().toLowerCase(Locale.ROOT).contains(mot))) // Recherche dans la lettre.
                .sorted(ordre).map(CandidatureResponse::depuis).toList(); // Trie et construit la réponse.
    }

    private ResponseStatusException erreur(int code, String message) { // Évite de répéter la création des erreurs.
        return new ResponseStatusException(HttpStatus.valueOf(code), message);
    }
}
