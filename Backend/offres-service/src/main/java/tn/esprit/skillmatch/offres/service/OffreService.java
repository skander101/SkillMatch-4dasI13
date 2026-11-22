package tn.esprit.skillmatch.offres.service; // Règles métier des offres.

import org.springframework.http.HttpStatus; // Codes des erreurs métier.
import org.springframework.context.ApplicationEventPublisher;
import tn.esprit.skillmatch.offres.event.EvenementOffres;
import org.springframework.stereotype.Service; // Service Spring.
import org.springframework.transaction.annotation.Transactional; // Gestion automatique des transactions.
import org.springframework.web.server.ResponseStatusException; // Erreurs HTTP explicites.
import tn.esprit.skillmatch.offres.dto.OffreRequest; // Formulaire validé.
import tn.esprit.skillmatch.offres.entity.Offre; // Entité persistée.
import tn.esprit.skillmatch.offres.enums.OffreStatut; // Statuts métier.
import tn.esprit.skillmatch.offres.repository.OffreRepository; // Accès aux offres.
import tn.esprit.skillmatch.offres.repository.CandidatureRepository; // Protection des candidatures existantes.
import tn.esprit.skillmatch.offres.security.IdentiteService; // Identité vérifiée côté serveur.
import tn.esprit.skillmatch.offres.security.IdentiteService.Identite; // Données de l'utilisateur connecté.
import java.time.LocalDate; // Date du jour.
import java.util.ArrayList; // Copie de la liste des compétences.
import java.util.List; // Listes de résultats.
import java.util.Comparator; // Compare les offres pour les trier.
import java.util.Locale; // Convertit les textes en minuscules.

@Service // Rend ce service injectable dans le contrôleur.
@Transactional // Une opération réussit entièrement ou est annulée.
public class OffreService { // Un seul service pour les trois rôles.
    private final OffreRepository offres; // Repository des offres.
    private final CandidatureRepository candidatures; // Repository des candidatures.
    private final IdentiteService identites; // Contrôle de l'utilisateur connecté.
    private final ApplicationEventPublisher evenements;

    public OffreService(OffreRepository offres, CandidatureRepository candidatures, IdentiteService identites, ApplicationEventPublisher evenements) { // Injection par constructeur.
        this.offres = offres; // Mémorise l'accès aux offres.
        this.candidatures = candidatures; // Mémorise l'accès aux candidatures.
        this.identites = identites; // Mémorise le service d'identité.
        this.evenements = evenements;
    } // Fin du constructeur.

    @Transactional(readOnly = true) // Cette méthode ne modifie pas la base.
    public List<Offre> lister(String authorization, String search, String sort) { // Adapte les résultats au rôle.
        Identite utilisateur = identites.courante(authorization); // Authentifie avant de consulter la base.
        if ("ADMIN".equals(utilisateur.role())) return rechercherEtTrier(offres.findAll(), search, sort); // ADMIN voit toutes les offres.
        if ("RECRUTEUR".equals(utilisateur.role())) return rechercherEtTrier(offres.findByRecruteurId(utilisateur.id()), search, sort); // RECRUTEUR voit ses offres.
        return rechercherEtTrier(offres.findByStatutAndDateExpirationGreaterThanEqual(OffreStatut.PUBLIEE, LocalDate.now()), search, sort); // CANDIDAT voit les offres ouvertes.
    } // Fin de la liste.

    @Transactional(readOnly = true) // Lecture seule.
    public List<Offre> mesOffres(String authorization, String search, String sort) { // Liste les offres du propriétaire connecté.
        Identite utilisateur = exigerGestionnaire(authorization); // Authentifie et exige ADMIN ou RECRUTEUR.
        return rechercherEtTrier(offres.findByRecruteurId(utilisateur.id()), search, sort); // Filtre par l'identifiant de session.
    } // Fin de mes offres.

    @Transactional(readOnly = true) // Lecture seule.
    public Offre consulter(Long id, String authorization) { // Lecture avec les contrôles communs.
        return trouverAccessible(id, identites.courante(authorization)); // Vérifie l'identité, l'existence et les droits.
    } // Fin de la consultation.

    public Offre creer(OffreRequest demande, String authorization) { // Crée un brouillon.
        Identite utilisateur = exigerGestionnaire(authorization); // Authentifie et exige ADMIN ou RECRUTEUR.
        if (demande.statut() != OffreStatut.BROUILLON) { // La publication passe ensuite par PUT.
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Créer d'abord un brouillon"); // Règle explicite.
        } // Fin du contrôle du statut initial.
        verifierDonnees(demande); // Refuse une date déjà passée pour un nouveau brouillon.
        Long proprietaire = utilisateur.id(); // Le recruteur est propriétaire de sa création.
        if ("ADMIN".equals(utilisateur.role())) { // ADMIN crée pour un recruteur réel.
            identites.verifierRecruteur(demande.recruteurId(), authorization); // Contrôle l'existence et le rôle côté serveur.
            proprietaire = demande.recruteurId(); // Affecte le recruteur vérifié.
        } else if (demande.recruteurId() != null && !proprietaire.equals(demande.recruteurId())) { // Détecte une usurpation de propriétaire.
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Impossible de choisir un autre recruteur"); // Réponse 403.
        } // Fin du choix du propriétaire.
        Offre offre = new Offre(); // Nouvel objet, sans identifiant envoyé par le client.
        remplir(offre, demande); // Copie uniquement les champs autorisés.
        offre.setRecruteurId(proprietaire); // Le propriétaire provient de la session ou de la vérification ADMIN.
        return offres.save(offre); // Enregistre et retourne l'offre créée.
    } // Fin de la création.

    public Offre modifier(Long id, OffreRequest demande, String authorization) { // Modifie une offre existante.
        Identite utilisateur = exigerGestionnaire(authorization); // Authentifie et exige ADMIN ou RECRUTEUR.
        Offre offre = trouverAccessible(id, utilisateur); // Charge l’offre et vérifie son propriétaire.
        if (demande.recruteurId() != null && !offre.getRecruteurId().equals(demande.recruteurId())) { // Le propriétaire est conservé même pour ADMIN.
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le propriétaire d'une offre ne peut pas être changé"); // Évite les transferts implicites.
        } // Fin du contrôle du propriétaire.
        verifierDonnees(demande); // Vérifie la cohérence entre statut et expiration.
        OffreStatut ancien = offre.getStatut(); // Détecte une vraie publication, pas une simple modification de texte.
        remplir(offre, demande); // Affecte les champs modifiables.
        Offre resultat = offres.save(offre);
        if (ancien != OffreStatut.PUBLIEE && offre.getStatut() == OffreStatut.PUBLIEE) {
            evenements.publishEvent(EvenementOffres.offrePubliee(offre));
        }
        return resultat;
    } // Fin de la modification.

    public void supprimer(Long id, String authorization) { // Supprime physiquement une offre sans candidature.
        Identite utilisateur = exigerGestionnaire(authorization); // Authentifie et exige ADMIN ou RECRUTEUR.
        Offre offre = trouverAccessible(id, utilisateur); // Charge l’offre et vérifie son propriétaire.
        if (candidatures.existsByOffreId(id)) { // Protège les candidatures associées.
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette offre possède des candidatures : fermez-la au lieu de la supprimer"); // Réponse 409.
        } // Fin de la protection.
        offres.delete(offre); // Supprime seulement cette offre.
        offres.flush(); // Détecte immédiatement un éventuel conflit de clé étrangère.
    } // Fin de la suppression.

    private Offre trouverAccessible(Long id, Identite utilisateur) { // Lecture et autorisation regroupées.
        Offre offre = offres.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Offre introuvable")); // 404 si absente.
        if ("CANDIDAT".equals(utilisateur.role())) { // Le candidat voit seulement les offres ouvertes.
            if (offre.getStatut() != OffreStatut.PUBLIEE || offre.estExpiree()) { // Vérifie le statut et la date.
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cette offre n'est pas ouverte"); // Réponse 403.
            } // Fin du contrôle candidat.
        } else if (!"ADMIN".equals(utilisateur.role()) && !offre.getRecruteurId().equals(utilisateur.id())) { // ADMIN peut tout consulter ; le recruteur reste propriétaire.
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cette offre appartient à un autre recruteur"); // Réponse 403.
        } // Fin du contrôle propriétaire.
        return offre; // Retourne uniquement une offre accessible.
    } // Fin de la lecture contrôlée.

    private Identite exigerGestionnaire(String authorization) { // Regroupe l'authentification et le contrôle de rôle.
        Identite utilisateur = identites.courante(authorization); // Résout l'identité une seule fois.
        if (!"ADMIN".equals(utilisateur.role()) && !"RECRUTEUR".equals(utilisateur.role())) { // Ne dépend pas des boutons Angular.
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Opération réservée aux recruteurs et administrateurs"); // Réponse 403.
        } // Fin du contrôle de rôle.
        return utilisateur; // Réutilise l’identité vérifiée dans l’opération.
    } // Fin du contrôle gestionnaire.

    private void verifierDonnees(OffreRequest demande) { // Vérifie la cohérence des données métier.
        boolean expiree = demande.dateExpiration().isBefore(LocalDate.now()); // Aujourd'hui reste valide.
        if (expiree) { // Toute date saisie doit être actuelle ou future, pour tous les statuts.
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La date d'expiration doit être aujourd’hui ou dans le futur"); // Réponse 400.
        } // Fin du contrôle des offres ouvertes.
        if (demande.statut() == OffreStatut.EXPIREE && !expiree) { // Le statut EXPIREE doit correspondre à une date passée.
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Une offre expirée doit avoir une date passée"); // Réponse 400.
        } // Fin du contrôle d'expiration.
        if (demande.salaire() != null && !Double.isFinite(demande.salaire())) { // Évite NaN et les valeurs infinies.
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Salaire invalide"); // Réponse 400.
        } // Fin du contrôle du salaire.
    } // Fin des validations métier.

    private void remplir(Offre offre, OffreRequest demande) { // Mapping explicite et facile à expliquer.
        offre.setTitre(demande.titre().trim()); // Nettoie le titre.
        offre.setDescription(demande.description()); // Met à jour la description.
        offre.setCompetences(new ArrayList<>(demande.competences())); // Copie la liste sans partager celle du formulaire.
        offre.setLocalisation(demande.localisation()); // Met à jour la localisation.
        offre.setContrat(demande.contrat()); // Met à jour le contrat.
        offre.setSalaire(demande.salaire()); // Met à jour le salaire.
        offre.setDateExpiration(demande.dateExpiration()); // Met à jour la date.
        offre.setStatut(demande.statut()); // Met à jour le statut.
        offre.setNomEntreprise(demande.nomEntreprise()); // Enregistre le nom libre.
        offre.setEntrepriseId(demande.entrepriseId()); // Conserve une simple référence Entreprise.
        offre.setTestRequisId(demande.testRequisId()); // Conserve une simple référence Test.
    } // Fin du mapping.

    private List<Offre> rechercherEtTrier(List<Offre> liste, String search, String sort) { // Une seule petite méthode pour les deux listes.
        String mot = search.trim().toLowerCase(Locale.ROOT); // Ignore la casse et les espaces autour du mot.
        Comparator<Offre> ordre = switch (sort) { // Choisit le tri croissant ou décroissant.
            case "titre" -> Comparator.comparing(Offre::getTitre, String.CASE_INSENSITIVE_ORDER); // Titre de A à Z.
            case "titreDesc" -> Comparator.comparing(Offre::getTitre, String.CASE_INSENSITIVE_ORDER).reversed(); // Titre de Z à A.
            case "salaire" -> Comparator.comparing(Offre::getSalaire, Comparator.nullsLast(Comparator.naturalOrder())); // Salaires croissants ; absents à la fin.
            case "salaireDesc" -> Comparator.comparing(Offre::getSalaire, Comparator.nullsLast(Comparator.reverseOrder())); // Salaires décroissants ; absents à la fin.
            case "dateExpiration" -> Comparator.comparing(Offre::getDateExpiration); // Expiration la plus proche d'abord.
            case "dateExpirationDesc" -> Comparator.comparing(Offre::getDateExpiration).reversed(); // Expiration la plus lointaine d’abord.
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tri : titre, salaire ou dateExpiration, avec Desc pour décroissant"); // Refuse un tri inconnu.
        }; // Fin du choix.
        return liste.stream() // Parcourt uniquement les offres déjà autorisées.
                .filter(offre -> offre.getTitre().toLowerCase(Locale.ROOT).contains(mot)) // Garde les titres contenant le mot.
                .sorted(ordre).toList(); // Trie puis retourne la liste.
    } // Fin de la recherche simple.
} // Fin du service.
