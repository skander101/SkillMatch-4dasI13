package tn.esprit.skillmatch.offres.service; // Règles métier des offres.

import org.springframework.http.HttpStatus; // Codes des erreurs métier.
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

@Service // Rend ce service injectable dans le contrôleur.
@Transactional // Une opération réussit entièrement ou est annulée.
public class OffreService { // Un seul service pour les trois rôles.
    private final OffreRepository offres; // Repository des offres.
    private final CandidatureRepository candidatures; // Repository des candidatures.
    private final IdentiteService identites; // Contrôle de l'utilisateur connecté.

    public OffreService(OffreRepository offres, CandidatureRepository candidatures, IdentiteService identites) { // Injection par constructeur.
        this.offres = offres; // Mémorise l'accès aux offres.
        this.candidatures = candidatures; // Mémorise l'accès aux candidatures.
        this.identites = identites; // Mémorise le service d'identité.
    } // Fin du constructeur.

    @Transactional(readOnly = true) // Cette méthode ne modifie pas la base.
    public List<Offre> lister(String authorization) { // Adapte les résultats au rôle.
        Identite utilisateur = identites.courante(authorization); // Authentifie avant de consulter la base.
        if ("ADMIN".equals(utilisateur.role())) return offres.findAll(); // ADMIN voit toutes les offres.
        if ("RECRUTEUR".equals(utilisateur.role())) return offres.findByRecruteurId(utilisateur.id()); // RECRUTEUR voit ses offres.
        return offres.findByStatutAndDateExpirationGreaterThanEqual(OffreStatut.PUBLIEE, LocalDate.now()); // CANDIDAT voit les offres ouvertes.
    } // Fin de la liste.

    @Transactional(readOnly = true) // Lecture seule.
    public List<Offre> mesOffres(String authorization) { // Liste les offres du propriétaire connecté.
        Identite utilisateur = identites.courante(authorization); // Identité validée par commun-service.
        exigerGestionnaire(utilisateur); // Réserve cette opération à ADMIN et RECRUTEUR.
        return offres.findByRecruteurId(utilisateur.id()); // Filtre par l'identifiant de session.
    } // Fin de mes offres.

    @Transactional(readOnly = true) // Lecture seule.
    public Offre consulter(Long id, String authorization) { // Consulte une seule offre.
        Identite utilisateur = identites.courante(authorization); // Authentifie la demande.
        Offre offre = trouver(id); // Retourne 404 si l'offre n'existe pas.
        if ("CANDIDAT".equals(utilisateur.role())) { // Applique la visibilité candidat.
            if (offre.getStatut() != OffreStatut.PUBLIEE || offre.estExpiree()) { // Masque les offres non ouvertes.
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cette offre n'est pas ouverte"); // Réponse 403.
            } // Fin du contrôle candidat.
        } else { // ADMIN ou RECRUTEUR.
            exigerProprietaire(offre, utilisateur); // ADMIN a accès ; RECRUTEUR doit être propriétaire.
        } // Fin des droits de lecture.
        return offre; // Le JSON contient seulement les champs de l'offre, sans relation candidature.
    } // Fin de la consultation.

    public Offre creer(OffreRequest demande, String authorization) { // Crée un brouillon.
        Identite utilisateur = identites.courante(authorization); // Vérifie la session.
        exigerGestionnaire(utilisateur); // Un candidat ne peut pas créer d'offre.
        if (demande.statut() != OffreStatut.BROUILLON) { // La publication passe ensuite par PUT.
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Créer d'abord un brouillon"); // Règle explicite.
        } // Fin du contrôle du statut initial.
        verifierDate(demande); // Refuse une date déjà passée pour un nouveau brouillon.
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
        Identite utilisateur = identites.courante(authorization); // Authentifie la demande.
        exigerGestionnaire(utilisateur); // Refuse les candidats.
        Offre offre = trouver(id); // Charge l'offre ou renvoie 404.
        exigerProprietaire(offre, utilisateur); // Empêche un recruteur de modifier l'offre d'un autre.
        if (demande.recruteurId() != null && !offre.getRecruteurId().equals(demande.recruteurId())) { // Le propriétaire est conservé même pour ADMIN.
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le propriétaire d'une offre ne peut pas être changé"); // Évite les transferts implicites.
        } // Fin du contrôle du propriétaire.
        verifierDate(demande); // Vérifie la cohérence entre statut et expiration.
        remplir(offre, demande); // Affecte les champs modifiables.
        return offres.save(offre); // Enregistre les modifications.
    } // Fin de la modification.

    public void supprimer(Long id, String authorization) { // Supprime physiquement une offre sans candidature.
        Identite utilisateur = identites.courante(authorization); // Vérifie la session.
        exigerGestionnaire(utilisateur); // Refuse les candidats.
        Offre offre = trouver(id); // Charge l'offre.
        exigerProprietaire(offre, utilisateur); // Vérifie le propriétaire ou le rôle ADMIN.
        if (candidatures.existsByOffreId(id)) { // Protège les candidatures associées.
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette offre possède des candidatures : fermez-la au lieu de la supprimer"); // Réponse 409.
        } // Fin de la protection.
        offres.delete(offre); // Supprime seulement cette offre.
        offres.flush(); // Détecte immédiatement un éventuel conflit de clé étrangère.
    } // Fin de la suppression.

    private Offre trouver(Long id) { // Recherche commune aux opérations sur un identifiant.
        return offres.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Offre introuvable")); // Réponse 404 si absente.
    } // Fin de la recherche interne.

    private void exigerGestionnaire(Identite utilisateur) { // Réserve les écritures aux rôles autorisés.
        if (!"ADMIN".equals(utilisateur.role()) && !"RECRUTEUR".equals(utilisateur.role())) { // Ne dépend pas des boutons Angular.
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Opération réservée aux recruteurs et administrateurs"); // Réponse 403.
        } // Fin du contrôle de rôle.
    } // Fin du contrôle gestionnaire.

    private void exigerProprietaire(Offre offre, Identite utilisateur) { // Applique la propriété côté backend.
        if (!"ADMIN".equals(utilisateur.role()) && !offre.getRecruteurId().equals(utilisateur.id())) { // ADMIN est autorisé sur toutes les offres.
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cette offre appartient à un autre recruteur"); // Réponse 403.
        } // Fin du contrôle de propriété.
    } // Fin du contrôle propriétaire.

    private void verifierDate(OffreRequest demande) { // Vérifie la cohérence des données métier.
        boolean expiree = demande.dateExpiration().isBefore(LocalDate.now()); // Aujourd'hui reste valide.
        if ((demande.statut() == OffreStatut.PUBLIEE || demande.statut() == OffreStatut.BROUILLON) && expiree) { // Une offre ouverte ou un nouveau brouillon ne doit pas être périmé.
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La date d'expiration ne peut pas être passée pour ce statut"); // Réponse 400.
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
        offre.setEntrepriseId(demande.entrepriseId()); // Conserve une simple référence Entreprise.
        offre.setTestRequisId(demande.testRequisId()); // Conserve une simple référence Test.
    } // Fin du mapping.
} // Fin du service.
