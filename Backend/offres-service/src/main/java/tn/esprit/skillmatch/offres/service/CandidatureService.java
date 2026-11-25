
package tn.esprit.skillmatch.offres.service;

import org.springframework.http.HttpStatus;
import org.springframework.context.ApplicationEventPublisher;
import tn.esprit.skillmatch.offres.event.EvenementOffres;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.skillmatch.offres.dto.*;
import tn.esprit.skillmatch.offres.entity.*;
import tn.esprit.skillmatch.offres.enums.*;
import tn.esprit.skillmatch.offres.repository.*;
import tn.esprit.skillmatch.offres.security.IdentiteService;
import tn.esprit.skillmatch.offres.security.IdentiteService.Identite;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.Comparator;
import java.util.Locale;


// ========== SERVICE MÉTIER DES CANDIDATURES ==========

// Déclare cette classe comme un service géré par Spring.
@Service

// Exécute les opérations dans une transaction MySQL.
@Transactional

// Regroupe les règles métier des candidatures.
public class CandidatureService {

    // Permet d'accéder aux candidatures dans MySQL.
    private final CandidatureRepository candidatures;

    // Permet de rechercher les offres dans MySQL.
    private final OffreRepository offres;

    // Permet d'utiliser les règles métier des offres.
    private final OffreService offreService;

    // Permet de vérifier l'utilisateur connecté.
    private final IdentiteService identites;

    // Permet de publier les événements du microservice.
    private final ApplicationEventPublisher evenements;


    // ========== CONSTRUCTEUR ==========

    // Reçoit les services nécessaires au fonctionnement de cette classe.
    public CandidatureService(CandidatureRepository candidatures, OffreRepository offres, IdentiteService identites, ApplicationEventPublisher evenements, OffreService offreService) {

        // Initialise l'accès aux candidatures.
        this.candidatures = candidatures;

        // Initialise l'accès aux offres.
        this.offres = offres;

        // Initialise le service des offres.
        this.offreService = offreService;

        // Initialise le service de vérification des utilisateurs.
        this.identites = identites;

        // Initialise l'outil de publication des événements.
        this.evenements = evenements;
    }


    // ========== 1. LISTER LES CANDIDATURES ==========

    // Indique que cette opération consulte les données sans les modifier.
    @Transactional(readOnly = true)

    // Retourne les candidatures autorisées avec recherche et tri.
    public List<CandidatureResponse> lister(String token, String search, String sort) {

        // Récupère l'identité et le rôle de l'utilisateur connecté.
        Identite u = identites.courante(token);

        // Choisit les candidatures à afficher selon le rôle.
        List<Candidature> liste = switch (u.role()) {

            // ADMIN peut récupérer toutes les candidatures.
            case "ADMIN" -> candidatures.findAll();

            // RECRUTEUR récupère les candidatures reçues pour ses offres.
            case "RECRUTEUR" -> candidatures.findByOffreRecruteurId(u.id());

            // Le candidat récupère seulement ses candidatures.
            default -> candidatures.findByUserId(u.id());
        };

        // Effectue la recherche et le tri sur la liste autorisée.
        return rechercherEtTrier(liste, search, sort);
    }


    // ========== 2. CONSULTER UNE CANDIDATURE ==========

    // Indique que cette opération consulte les données sans les modifier.
    @Transactional(readOnly = true)

    // Retourne les détails d'une candidature accessible.
    public CandidatureResponse consulter(Long id, String token) {

        // Vérifie l'accès puis prépare la réponse pour Angular.
        return CandidatureResponse.depuis(trouverAccessible(id, identites.courante(token)));
    }


    // ========== 3. CRÉER UNE CANDIDATURE ==========

    // Crée une candidature après vérification des règles métier.
    public CandidatureResponse creer(CandidatureRequest d, String token) {

        // Récupère l'identité et le rôle de l'utilisateur connecté.
        Identite u = identites.courante(token);

        // Interdit à un recruteur de déposer une candidature.
        if ("RECRUTEUR".equals(u.role())) throw erreur(403, "Un recruteur ne peut pas postuler pour un candidat");

        // Utilise par défaut l'identifiant de l'utilisateur connecté.
        Long candidat = u.id();

        // Vérifie si la demande est faite par un administrateur.
        if ("ADMIN".equals(u.role())) {

            // Vérifie que le candidat choisi par ADMIN existe bien.
            identites.verifierCandidat(d.userId(), token);

            // Utilise l'identifiant du candidat choisi par ADMIN.
            candidat = d.userId();

            // Empêche un candidat de postuler au nom d'un autre.
        } else if (d.userId() != null && !candidat.equals(d.userId())) {

            // Refuse cette candidature avec une erreur 403.
            throw erreur(403, "Impossible de postuler au nom d'un autre candidat");
        }

        // Recherche l'offre et renvoie 404 si elle n'existe pas.
        Offre offre = offres.findById(d.offreId()).orElseThrow(() -> erreur(404, "Offre introuvable"));

        // Vérifie que l'offre est publiée et non expirée.
        if (offre.getStatut() != OffreStatut.PUBLIEE || offreService.estExpiree(offre)) throw erreur(409, "Cette offre n'est pas ouverte");

        // Empêche de postuler deux fois à la même offre.
        if (candidatures.existsByUserIdAndOffreId(candidat, offre.getId())) throw erreur(409, "Vous avez déjà postulé à cette offre");

        // Crée une nouvelle candidature.
        Candidature c = new Candidature();

        // Associe la candidature à l'offre choisie.
        c.setOffre(offre);

        // Associe la candidature au candidat vérifié.
        c.setUserId(candidat);

        // Ajoute la lettre de motivation.
        c.setLettre(d.lettre());

        // Ajoute l'identifiant du CV, s'il existe.
        c.setCvId(d.cvId());

        // Enregistre immédiatement la candidature dans MySQL.
        Candidature enregistree = candidatures.saveAndFlush(c);

        // Publie un événement signalant le dépôt de candidature.
        evenements.publishEvent(EvenementOffres.candidatureDeposee(enregistree));

        // Retourne les informations de la candidature créée.
        return CandidatureResponse.depuis(enregistree);
    }


    // ========== 4. MODIFIER UNE CANDIDATURE ==========

    // Modifie une candidature selon les droits de l'utilisateur.
    public CandidatureResponse modifier(Long id, CandidatureUpdate d, String token) {

        // Récupère l'identité et le rôle de l'utilisateur connecté.
        Identite u = identites.courante(token);

        // Recherche la candidature et vérifie le droit d'y accéder.
        Candidature c = trouverAccessible(id, u);

        // Vérifie quelles modifications sont autorisées.
        verifierModification(c, d, u);

        // Conserve l'ancien statut pour détecter un changement.
        CandidatureStatut ancien = c.getStatut();

        // Modifie la lettre uniquement si une nouvelle est fournie.
        if (d.lettre() != null) c.setLettre(d.lettre());

        // Modifie le CV uniquement si un identifiant est fourni.
        if (d.cvId() != null) c.setCvId(d.cvId());

        // Modifie le statut uniquement s'il est renseigné.
        if (d.statut() != null) c.changerStatut(d.statut());

        // Modifie la date d'entretien si elle est renseignée.
        if (d.dateEntretien() != null) c.setDateEntretien(d.dateEntretien());

        // Modifie la note si une nouvelle note est fournie.
        if (d.noteRecruteur() != null) c.setNoteRecruteur(d.noteRecruteur());

        // Modifie le commentaire s'il est renseigné.
        if (d.commentaireRecruteur() != null) c.setCommentaireRecruteur(d.commentaireRecruteur());

        // Enregistre les modifications dans MySQL.
        Candidature enregistree = candidatures.save(c);

        // Publie un événement seulement si le statut a changé.
        if (ancien != c.getStatut()) evenements.publishEvent(EvenementOffres.statutChange(c, ancien));

        // Retourne les informations de la candidature modifiée.
        return CandidatureResponse.depuis(enregistree);
    }


    // ========== 5. SUPPRIMER OU RETIRER UNE CANDIDATURE ==========

    // Supprime ou retire une candidature selon le rôle.
    public void supprimer(Long id, String token) {

        // Récupère l'identité et le rôle de l'utilisateur connecté.
        Identite u = identites.courante(token);

        // Recherche la candidature et vérifie le droit d'y accéder.
        Candidature c = trouverAccessible(id, u);

        // Interdit la suppression aux recruteurs.
        if ("RECRUTEUR".equals(u.role())) throw erreur(403, "Le recruteur ne peut pas supprimer une candidature");

        // ADMIN supprime définitivement la candidature.
        if ("ADMIN".equals(u.role())) candidatures.delete(c);

            // Sinon, applique le retrait de candidature.
        else {

            // Autorise le retrait uniquement si la candidature est RECUE.
            if (c.getStatut() != CandidatureStatut.RECUE) throw erreur(409, "Seule une candidature RECUE peut être retirée");

            // Passe la candidature au statut RETIREE.
            c.changerStatut(CandidatureStatut.RETIREE);

            // Enregistre le retrait dans MySQL.
            candidatures.save(c);

            // Signale le changement de statut aux autres services.
            evenements.publishEvent(EvenementOffres.statutChange(c, CandidatureStatut.RECUE));
        }
    }


    // ========== 6. VÉRIFIER L'ACCÈS À UNE CANDIDATURE ==========

    // Recherche une candidature et contrôle qui peut y accéder.
    private Candidature trouverAccessible(Long id, Identite u) {

        // Récupère la candidature ou renvoie une erreur 404.
        Candidature c = candidatures.findById(id).orElseThrow(() -> erreur(404, "Candidature introuvable"));

        // Autorise ADMIN ou le candidat propriétaire.
        boolean autorise = "ADMIN".equals(u.role()) || ("CANDIDAT".equals(u.role()) && c.getUserId().equals(u.id()))

            // Autorise aussi le recruteur propriétaire de l'offre.
            || ("RECRUTEUR".equals(u.role()) && c.getOffre().getRecruteurId().equals(u.id()));

        // Refuse l'accès si l'utilisateur n'est pas autorisé.
        if (!autorise) throw erreur(403, "Cette candidature ne vous est pas accessible");

        // Retourne la candidature autorisée.
        return c;
    }


    // ========== 7. CONTRÔLER LES MODIFICATIONS SELON LE RÔLE ==========

    // Contrôle les modifications autorisées selon le rôle.
    private void verifierModification(Candidature c, CandidatureUpdate d, Identite u) {

        // Applique les règles de modification du candidat.
        if ("CANDIDAT".equals(u.role())) {

            // Le candidat ne peut modifier qu'une candidature RECUE.
            if (c.getStatut() != CandidatureStatut.RECUE) throw erreur(409, "Modification possible uniquement au statut RECUE");

            // Vérifie que le candidat ne touche pas aux champs du recruteur.
            if (d.statut() != null || d.dateEntretien() != null || d.noteRecruteur() != null || d.commentaireRecruteur() != null)

                // Refuse une modification non autorisée au candidat.
                throw erreur(403, "Le candidat peut modifier seulement sa lettre et son cvId");
        }

        // Applique les règles de modification du recruteur.
        if ("RECRUTEUR".equals(u.role())) {

            // Interdit de traiter une candidature déjà retirée.
            if (c.getStatut() == CandidatureStatut.RETIREE) throw erreur(409, "Cette candidature a été retirée");

            // Empêche le recruteur de modifier la lettre ou le CV.
            if (d.lettre() != null || d.cvId() != null) throw erreur(403, "La lettre et le CV appartiennent au candidat");

            // Vérifie que le nouveau statut appartient aux statuts autorisés.
            if (d.statut() != null && !Set.of(CandidatureStatut.PRESELECTIONNEE, CandidatureStatut.ENTRETIEN_PLANIFIE,

                // Refuse les autres statuts pour le recruteur.
                CandidatureStatut.ACCEPTEE, CandidatureStatut.REFUSEE).contains(d.statut())) throw erreur(403, "Statut réservé au candidat ou à ADMIN");
        }

        // Contrôle la lettre pour le candidat ou ADMIN.
        if (!"RECRUTEUR".equals(u.role())) {

            // Prend la nouvelle lettre, ou garde l'ancienne si elle est absente.
            String lettre = d.lettre() == null ? c.getLettre() : d.lettre();

            // Vérifie que la lettre respecte la longueur autorisée.
            if (lettre == null || lettre.trim().length() < 20 || lettre.trim().length() > 10000)

                // Refuse une lettre absente ou de longueur incorrecte.
                throw erreur(400, "La lettre est obligatoire : 20 à 10000 caractères");
        }

        // Prend la nouvelle date d'entretien ou conserve l'ancienne.
        LocalDateTime entretien = d.dateEntretien() == null ? c.getDateEntretien() : d.dateEntretien();

        // Vérifie les informations de traitement pour ADMIN et RECRUTEUR.
        if (!"CANDIDAT".equals(u.role())) {

            // Prend la note reçue ou conserve la précédente.
            Integer note = d.noteRecruteur() == null ? c.getNoteRecruteur() : d.noteRecruteur();

            // Prend le commentaire reçu ou conserve le précédent.
            String commentaire = d.commentaireRecruteur() == null ? c.getCommentaireRecruteur() : d.commentaireRecruteur();

            // Vérifie que l'entretien existe et n'est pas dans le passé.
            if (entretien == null || entretien.toLocalDate().isBefore(LocalDateTime.now().toLocalDate()))

                // Refuse une date d'entretien absente ou passée.
                throw erreur(400, "La date d'entretien est obligatoire, aujourd'hui ou dans le futur");

            // Refuse une note absente ou négative.
            if (note == null || note < 0) throw erreur(400, "La note est obligatoire et positive ou nulle");

            // Vérifie la présence et la longueur du commentaire.
            if (commentaire == null || commentaire.trim().length() < 5 || commentaire.trim().length() > 10000)

                // Refuse un commentaire absent ou de longueur incorrecte.
                throw erreur(400, "Le commentaire est obligatoire : 5 à 10000 caractères");
        }
    }


    // ========== 8. RECHERCHER ET TRIER ==========

    // Filtre et trie les candidatures à afficher.
    private List<CandidatureResponse> rechercherEtTrier(List<Candidature> liste, String search, String sort) {

        // Nettoie le mot de recherche et ignore les majuscules.
        String mot = search.trim().toLowerCase(Locale.ROOT);

        // Choisit l'ordre de tri demandé.
        Comparator<Candidature> ordre = switch (sort) {

            // Trie les candidatures de la plus ancienne à la plus récente.
            case "dateCandidature" -> Comparator.comparing(Candidature::getDateCandidature);

            // Trie les candidatures de la plus récente à la plus ancienne.
            case "dateCandidatureDesc" -> Comparator.comparing(Candidature::getDateCandidature).reversed();

            // Refuse une valeur de tri inconnue.
            default -> throw erreur(400, "Tri : dateCandidature ou dateCandidatureDesc");
        };

        // Commence à parcourir la liste des candidatures.
        return liste.stream()

            // Garde les candidatures dont la lettre correspond à la recherche.
            .filter(c -> mot.isEmpty() || (c.getLettre() != null && c.getLettre().toLowerCase(Locale.ROOT).contains(mot)))

            // Trie les résultats et les convertit en réponses JSON.
            .sorted(ordre).map(CandidatureResponse::depuis).toList();
    }


    // ========== 9. PRÉPARER UNE ERREUR HTTP ==========

    // Prépare une erreur HTTP avec un code et un message.
    private ResponseStatusException erreur(int code, String message) {

        // Crée et retourne l'erreur HTTP demandée.
        return new ResponseStatusException(HttpStatus.valueOf(code), message);
    }
}
