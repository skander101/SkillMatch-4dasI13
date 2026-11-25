
package tn.esprit.skillmatch.offres.service;

import org.springframework.http.HttpStatus;
import org.springframework.context.ApplicationEventPublisher;
import tn.esprit.skillmatch.offres.event.EvenementOffres;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.skillmatch.offres.dto.OffreRequest;
import tn.esprit.skillmatch.offres.entity.Offre;
import tn.esprit.skillmatch.offres.enums.OffreStatut;
import tn.esprit.skillmatch.offres.repository.OffreRepository;
import tn.esprit.skillmatch.offres.repository.CandidatureRepository;
import tn.esprit.skillmatch.offres.security.IdentiteService;
import tn.esprit.skillmatch.offres.security.IdentiteService.Identite;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Locale;


// ========== SERVICE MÉTIER DES OFFRES ==========

// Déclare cette classe comme un service Spring.
@Service

// Gère automatiquement les transactions MySQL.
@Transactional

// Contient les règles métier des offres d'emploi.
public class OffreService {

    // Permet d'accéder aux offres dans MySQL.
    private final OffreRepository offres;

    // Permet de vérifier les candidatures associées.
    private final CandidatureRepository candidatures;

    // Permet de vérifier l'identité des utilisateurs.
    private final IdentiteService identites;

    // Permet de publier les événements.
    private final ApplicationEventPublisher evenements;


    // ========== CONSTRUCTEUR ==========

    // Injecte les outils nécessaires au service.
    public OffreService(OffreRepository offres, CandidatureRepository candidatures, IdentiteService identites, ApplicationEventPublisher evenements) {

        // Initialise le repository des offres.
        this.offres = offres;

        // Initialise le repository des candidatures.
        this.candidatures = candidatures;

        // Initialise le service d'identité.
        this.identites = identites;

        // Initialise la publication des événements.
        this.evenements = evenements;
    }


    // ========== 1. VÉRIFIER L'EXPIRATION ==========

    // Vérifie si la date limite d'une offre est passée.
    public Boolean estExpiree(Offre offre) {

        // Retourne true si la date d'expiration est avant aujourd'hui.
        return offre.getDateExpiration() != null && offre.getDateExpiration().isBefore(LocalDate.now());
    }


    // ========== 2. CALCULER LE SCORE ==========

    // Calcule le pourcentage de compétences correspondantes.
    public Double calculerScore(Offre offre, List<String> competencesProfil) {

        // Récupère les compétences demandées par l'offre.
        List<String> competences = offre.getCompetences();

        // Vérifie si les compétences nécessaires sont absentes.
        if (competences == null || competences.isEmpty() || competencesProfil == null) {

            // Retourne zéro si la comparaison est impossible.
            return 0.0;
        }

        // Nettoie les compétences de l'offre et supprime les doublons.
        Set<String> requises = normaliser(competences);

        // Vérifie si aucune compétence valide n'est demandée.
        if (requises.isEmpty()) {

            // Retourne zéro pour éviter une division par zéro.
            return 0.0;
        }

        // Nettoie les compétences du candidat.
        Set<String> profil = normaliser(competencesProfil);

        // Compte les compétences communes entre l'offre et le candidat.
        long communes = requises.stream().filter(profil::contains).count();

        // Calcule et retourne le score en pourcentage.
        return 100.0 * communes / requises.size();
    }


    // ========== 3. NORMALISER LES COMPÉTENCES ==========

    // Nettoie les compétences pour faciliter leur comparaison.
    private Set<String> normaliser(List<String> valeurs) {

        // Parcourt les compétences de la liste.
        return valeurs.stream()

            // Ignore les compétences absentes ou vides.
            .filter(valeur -> valeur != null && !valeur.isBlank())

            // Supprime les espaces et transforme en minuscules.
            .map(valeur -> valeur.trim().toLowerCase(Locale.ROOT))

            // Regroupe les compétences sans doublons.
            .collect(Collectors.toSet());
    }


    // ========== 4. LISTER LES OFFRES ==========

    // Indique que cette méthode consulte seulement les données.
    @Transactional(readOnly = true)

    // Récupère les offres accessibles selon le rôle.
    public List<Offre> lister(String authorization, String search, String sort) {

        // Vérifie l'identité de l'utilisateur connecté.
        Identite utilisateur = identites.courante(authorization);

        // ADMIN peut voir toutes les offres.
        if ("ADMIN".equals(utilisateur.role())) return rechercherEtTrier(offres.findAll(), search, sort);

        // RECRUTEUR peut voir ses propres offres.
        if ("RECRUTEUR".equals(utilisateur.role())) return rechercherEtTrier(offres.findByRecruteurId(utilisateur.id()), search, sort);

        // CANDIDAT peut voir les offres publiées et non expirées.
        return rechercherEtTrier(offres.findByStatutAndDateExpirationGreaterThanEqual(OffreStatut.PUBLIEE, LocalDate.now()), search, sort);
    }


    // ========== 5. LISTER MES OFFRES ==========

    // Indique que cette méthode consulte seulement les données.
    @Transactional(readOnly = true)

    // Récupère les offres appartenant au recruteur connecté.
    public List<Offre> mesOffres(String authorization, String search, String sort) {

        // Vérifie que l'utilisateur est ADMIN ou RECRUTEUR.
        Identite utilisateur = exigerGestionnaire(authorization);

        // Récupère les offres du propriétaire avec recherche et tri.
        return rechercherEtTrier(offres.findByRecruteurId(utilisateur.id()), search, sort);
    }


    // ========== 6. CONSULTER UNE OFFRE ==========

    // Indique que cette méthode consulte seulement les données.
    @Transactional(readOnly = true)

    // Récupère les détails d'une offre.
    public Offre consulter(Long id, String authorization) {

        // Recherche l'offre et vérifie les droits d'accès.
        return trouverAccessible(id, identites.courante(authorization));
    }


    // ========== 7. CRÉER UNE OFFRE ==========

    // Crée une nouvelle offre en brouillon.
    public Offre creer(OffreRequest demande, String authorization) {

        // Vérifie que l'utilisateur peut gérer les offres.
        Identite utilisateur = exigerGestionnaire(authorization);

        // Vérifie que la nouvelle offre est en BROUILLON.
        if (demande.statut() != OffreStatut.BROUILLON) {

            // Refuse la création avec un autre statut.
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Créer d'abord un brouillon");
        }

        // Vérifie la validité des données du formulaire.
        verifierDonnees(demande);

        // Récupère l'identifiant du recruteur connecté.
        Long proprietaire = utilisateur.id();

        // Vérifie si la création est effectuée par ADMIN.
        if ("ADMIN".equals(utilisateur.role())) {

            // Vérifie que le recruteur choisi existe.
            identites.verifierRecruteur(demande.recruteurId(), authorization);

            // Affecte l'offre au recruteur choisi.
            proprietaire = demande.recruteurId();

            // Empêche un recruteur de choisir un autre propriétaire.
        } else if (demande.recruteurId() != null && !proprietaire.equals(demande.recruteurId())) {

            // Refuse cette opération avec une erreur 403.
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Impossible de choisir un autre recruteur");
        }

        // Crée un nouvel objet Offre.
        Offre offre = new Offre();

        // Remplit l'offre avec les données du formulaire.
        remplir(offre, demande);

        // Associe l'offre au propriétaire vérifié.
        offre.setRecruteurId(proprietaire);

        // Enregistre la nouvelle offre dans MySQL.
        return offres.save(offre);
    }


    // ========== 8. MODIFIER OU PUBLIER UNE OFFRE ==========

    // Modifie une offre existante.
    public Offre modifier(Long id, OffreRequest demande, String authorization) {

        // Vérifie que l'utilisateur peut gérer les offres.
        Identite utilisateur = exigerGestionnaire(authorization);

        // Récupère l'offre et vérifie les droits d'accès.
        Offre offre = trouverAccessible(id, utilisateur);

        // Empêche de changer le propriétaire de l'offre.
        if (demande.recruteurId() != null && !offre.getRecruteurId().equals(demande.recruteurId())) {

            // Refuse le changement de propriétaire.
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le propriétaire d'une offre ne peut pas être changé");
        }

        // Vérifie les données reçues.
        verifierDonnees(demande);

        // Conserve l'ancien statut de l'offre.
        OffreStatut ancien = offre.getStatut();

        // Met à jour les informations de l'offre.
        remplir(offre, demande);

        // Enregistre les modifications dans MySQL.
        Offre resultat = offres.save(offre);

        // Vérifie si l'offre vient d'être publiée.
        if (ancien != OffreStatut.PUBLIEE && offre.getStatut() == OffreStatut.PUBLIEE) {

            // Publie un événement pour signaler la publication.
            evenements.publishEvent(EvenementOffres.offrePubliee(offre));
        }

        // Retourne l'offre modifiée.
        return resultat;
    }


    // ========== 9. SUPPRIMER UNE OFFRE ==========

    // Supprime une offre si elle ne possède aucune candidature.
    public void supprimer(Long id, String authorization) {

        // Vérifie que l'utilisateur peut gérer les offres.
        Identite utilisateur = exigerGestionnaire(authorization);

        // Récupère l'offre et vérifie les droits d'accès.
        Offre offre = trouverAccessible(id, utilisateur);

        // Vérifie si des candidats ont déjà postulé à cette offre.
        if (candidatures.existsByOffreId(id)) {

            // Refuse la suppression si des candidatures existent.
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette offre possède des candidatures : fermez-la au lieu de la supprimer");
        }

        // Supprime l'offre de MySQL.
        offres.delete(offre);

        // Exécute immédiatement la suppression en base.
        offres.flush();
    }


    // ========== 10. VÉRIFIER L'ACCÈS À UNE OFFRE ==========

    // Recherche une offre et vérifie les droits d'accès.
    private Offre trouverAccessible(Long id, Identite utilisateur) {

        // Recherche l'offre ou retourne une erreur 404.
        Offre offre = offres.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Offre introuvable"));

        // Vérifie si l'utilisateur est un candidat.
        if ("CANDIDAT".equals(utilisateur.role())) {

            // Vérifie que l'offre est publiée et non expirée.
            if (offre.getStatut() != OffreStatut.PUBLIEE || estExpiree(offre)) {

                // Refuse l'accès à une offre non ouverte.
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cette offre n'est pas ouverte");
            }

            // Vérifie que le recruteur est propriétaire de l'offre.
        } else if (!"ADMIN".equals(utilisateur.role()) && !offre.getRecruteurId().equals(utilisateur.id())) {

            // Refuse l'accès aux offres des autres recruteurs.
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cette offre appartient à un autre recruteur");
        }

        // Retourne l'offre autorisée.
        return offre;
    }


    // ========== 11. VÉRIFIER LE RÔLE ==========

    // Vérifie que l'utilisateur peut gérer les offres.
    private Identite exigerGestionnaire(String authorization) {

        // Récupère l'identité de l'utilisateur connecté.
        Identite utilisateur = identites.courante(authorization);

        // Autorise uniquement les recruteurs et les administrateurs.
        if (!"ADMIN".equals(utilisateur.role()) && !"RECRUTEUR".equals(utilisateur.role())) {

            // Refuse l'opération avec une erreur 403.
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Opération réservée aux recruteurs et administrateurs");
        }

        // Retourne l'identité de l'utilisateur autorisé.
        return utilisateur;
    }


    // ========== 12. VÉRIFIER LES DONNÉES ==========

    // Vérifie la validité des données métier de l'offre.
    private void verifierDonnees(OffreRequest demande) {

        // Vérifie si la date d'expiration est dans le passé.
        boolean expiree = demande.dateExpiration().isBefore(LocalDate.now());

        // Refuse une date d'expiration passée.
        if (expiree) {

            // Retourne une erreur 400.
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La date d'expiration doit être aujourd’hui ou dans le futur");
        }

        // Vérifie la cohérence du statut EXPIREE avec la date.
        if (demande.statut() == OffreStatut.EXPIREE && !expiree) {

            // Refuse une offre marquée EXPIREE avec une date non passée.
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Une offre expirée doit avoir une date passée");
        }

        // Vérifie que le salaire n'est pas infini ou invalide.
        if (demande.salaire() != null && !Double.isFinite(demande.salaire())) {

            // Retourne une erreur 400 si le salaire est invalide.
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Salaire invalide");
        }
    }


    // ========== 13. REMPLIR L'OFFRE ==========

    // Copie les informations du formulaire dans l'offre.
    private void remplir(Offre offre, OffreRequest demande) {

        // Enregistre le titre sans espaces inutiles.
        offre.setTitre(demande.titre().trim());

        // Enregistre la description.
        offre.setDescription(demande.description());

        // Copie les compétences dans une nouvelle liste.
        offre.setCompetences(new ArrayList<>(demande.competences()));

        // Enregistre la localisation.
        offre.setLocalisation(demande.localisation());

        // Enregistre le type de contrat.
        offre.setContrat(demande.contrat());

        // Enregistre le salaire.
        offre.setSalaire(demande.salaire());

        // Enregistre la date d'expiration.
        offre.setDateExpiration(demande.dateExpiration());

        // Enregistre le statut de l'offre.
        offre.setStatut(demande.statut());

        // Enregistre le nom de l'entreprise.
        offre.setNomEntreprise(demande.nomEntreprise());

        // Enregistre l'identifiant de l'entreprise.
        offre.setEntrepriseId(demande.entrepriseId());

        // Enregistre l'identifiant du test technique.
        offre.setTestRequisId(demande.testRequisId());
    }


    // ========== 14. RECHERCHER ET TRIER LES OFFRES ==========

    // Recherche et trie les offres à afficher.
    private List<Offre> rechercherEtTrier(List<Offre> liste, String search, String sort) {

        // Nettoie le mot recherché et ignore les majuscules.
        String mot = search.trim().toLowerCase(Locale.ROOT);

        // Choisit l'ordre de tri demandé.
        Comparator<Offre> ordre = switch (sort) {

            // Trie les titres de A à Z.
            case "titre" -> Comparator.comparing(Offre::getTitre, String.CASE_INSENSITIVE_ORDER);

            // Trie les titres de Z à A.
            case "titreDesc" -> Comparator.comparing(Offre::getTitre, String.CASE_INSENSITIVE_ORDER).reversed();

            // Trie les salaires du plus petit au plus grand.
            case "salaire" -> Comparator.comparing(Offre::getSalaire, Comparator.nullsLast(Comparator.naturalOrder()));

            // Trie les salaires du plus grand au plus petit.
            case "salaireDesc" -> Comparator.comparing(Offre::getSalaire, Comparator.nullsLast(Comparator.reverseOrder()));

            // Trie les offres par date d'expiration croissante.
            case "dateExpiration" -> Comparator.comparing(Offre::getDateExpiration);

            // Trie les offres par date d'expiration décroissante.
            case "dateExpirationDesc" -> Comparator.comparing(Offre::getDateExpiration).reversed();

            // Refuse les valeurs de tri inconnues.
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tri : titre, salaire ou dateExpiration, avec Desc pour décroissant");
        };

        // Parcourt les offres accessibles.
        return liste.stream()

            // Garde les offres dont le titre contient le mot recherché.
            .filter(offre -> offre.getTitre().toLowerCase(Locale.ROOT).contains(mot))

            // Trie et retourne la liste des offres.
            .sorted(ordre).toList();
    }
}
