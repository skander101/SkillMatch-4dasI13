package tn.esprit.skillmatch.offres.controller; // Points d'entrée REST des offres.

import jakarta.validation.Valid; // Valide le formulaire avant le service.
import org.springframework.http.HttpStatus; // Codes 201 et 204.
import org.springframework.web.bind.annotation.*; // Annotations REST.
import tn.esprit.skillmatch.offres.dto.OffreRequest; // Données acceptées.
import tn.esprit.skillmatch.offres.entity.Offre; // Réponse JSON sans relation candidature.
import tn.esprit.skillmatch.offres.service.OffreService; // Règles métier.
import java.util.List; // Réponse des listes.

@RestController // Transforme les réponses en JSON.
@RequestMapping("/skillmatch/offres") // Préfixe commun à toutes les routes de ce contrôleur.
public class OffreController { // Un seul contrôleur pour les trois rôles.
    private final OffreService service; // Service métier partagé.

    public OffreController(OffreService service) { // Injection du service.
        this.service = service; // Mémorise le service.
    } // Fin du constructeur.

    @GetMapping // GET /skillmatch/offres.
    public List<Offre> lister(@RequestParam(defaultValue = "") String search, @RequestParam(defaultValue = "titre") String sort, // Deux paramètres simples de l'URL.
                              @RequestHeader(value = "Authorization", required = false) String token) { // JWT de l'utilisateur.
        return service.lister(token, search, sort); // Recherche seulement dans les offres autorisées.
    } // Fin de la liste.

    @GetMapping("/mes-offres") // GET /skillmatch/offres/mes-offres.
    public List<Offre> mesOffres(@RequestParam(defaultValue = "") String search, @RequestParam(defaultValue = "titre") String sort, // Même recherche et tri.
                                 @RequestHeader(value = "Authorization", required = false) String token) { // Identité du propriétaire.
        return service.mesOffres(token, search, sort); // Ne lit aucun userId depuis le client.
    } // Fin de mes offres.

    @GetMapping("/{id}") // GET /skillmatch/offres/12.
    public Offre consulter(@PathVariable Long id, @RequestHeader(value = "Authorization", required = false) String token) { // Lit l'identifiant dans l'URL.
        return service.consulter(id, token); // Le service vérifie la visibilité.
    } // Fin de la consultation.

    @PostMapping // POST /skillmatch/offres.
    @ResponseStatus(HttpStatus.CREATED) // Retourne 201 en cas de création.
    public Offre creer(@Valid @RequestBody OffreRequest demande, @RequestHeader(value = "Authorization", required = false) String token) { // Valide le JSON.
        return service.creer(demande, token); // Crée le brouillon avec un propriétaire fiable.
    } // Fin de la création.

    @PutMapping("/{id}") // PUT /skillmatch/offres/12.
    public Offre modifier(@PathVariable Long id, @Valid @RequestBody OffreRequest demande, @RequestHeader(value = "Authorization", required = false) String token) { // Valide les données.
        return service.modifier(id, demande, token); // Modifie ou publie l'offre.
    } // Fin de la modification.

    @DeleteMapping("/{id}") // DELETE /skillmatch/offres/12.
    @ResponseStatus(HttpStatus.NO_CONTENT) // Retourne 204 sans corps si l'offre est supprimée.
    public void supprimer(@PathVariable Long id, @RequestHeader(value = "Authorization", required = false) String token) { // Identifiant à supprimer.
        service.supprimer(id, token); // Vérifie les droits et protège les candidatures.
    } // Fin de la suppression.
} // Fin du contrôleur.
