
package tn.esprit.skillmatch.offres.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import tn.esprit.skillmatch.offres.dto.OffreRequest;
import tn.esprit.skillmatch.offres.entity.Offre;
import tn.esprit.skillmatch.offres.service.OffreService;
import java.util.List;


// ========== CONTROLLER DES OFFRES ==========

// Indique que cette classe gère les requêtes REST et retourne du JSON.
@RestController

// Définit l'URL principale des offres.
@RequestMapping("/skillmatch/offres")

// Classe qui gère les opérations sur les offres d'emploi.
public class OffreController {


    // ========== DÉCLARATION DU SERVICE ==========

    // Déclare le service qui contient les règles métier des offres.
    private final OffreService service;


    // ========== CONSTRUCTEUR ==========

    // Injecte automatiquement le service dans le contrôleur.
    public OffreController(OffreService service) {

        // Initialise le service utilisé par le contrôleur.
        this.service = service;
    }


    // ========== 1. LISTER LES OFFRES ==========

    // Permet de récupérer les offres avec une requête GET.
    @GetMapping

    // Récupère la liste des offres avec recherche et tri.
    public List<Offre> lister(@RequestParam(defaultValue = "") String search, @RequestParam(defaultValue = "titre") String sort,
                              @RequestHeader(value = "Authorization", required = false) String token) {

        // Appelle le service pour retourner les offres autorisées.
        return service.lister(token, search, sort);
    }


    // ========== 2. LISTER MES OFFRES ==========

    // Permet de récupérer les offres du recruteur connecté.
    @GetMapping("/mes-offres")

    // Récupère les offres du recruteur avec recherche et tri.
    public List<Offre> mesOffres(@RequestParam(defaultValue = "") String search, @RequestParam(defaultValue = "titre") String sort,
                                 @RequestHeader(value = "Authorization", required = false) String token) {

        // Appelle le service pour récupérer les offres du recruteur.
        return service.mesOffres(token, search, sort);
    }


    // ========== 3. CONSULTER UNE OFFRE ==========

    // Permet de récupérer une offre par son identifiant.
    @GetMapping("/{id}")

    // Récupère les détails d'une offre.
    public Offre consulter(@PathVariable Long id, @RequestHeader(value = "Authorization", required = false) String token) {

        // Appelle le service pour consulter l'offre demandée.
        return service.consulter(id, token);
    }


    // ========== 4. CRÉER UNE OFFRE ==========

    // Permet de créer une offre avec une requête POST.
    @PostMapping

    // Retourne le code HTTP 201 si la création réussit.
    @ResponseStatus(HttpStatus.CREATED)

    // Reçoit et valide les informations d'une nouvelle offre.
    public Offre creer(@Valid @RequestBody OffreRequest demande, @RequestHeader(value = "Authorization", required = false) String token) {

        // Appelle le service pour créer l'offre en brouillon.
        return service.creer(demande, token);
    }


    // ========== 5. MODIFIER UNE OFFRE ==========

    // Permet de modifier une offre avec une requête PUT.
    @PutMapping("/{id}")

    // Reçoit et valide les informations à modifier.
    public Offre modifier(@PathVariable Long id, @Valid @RequestBody OffreRequest demande, @RequestHeader(value = "Authorization", required = false) String token) {

        // Appelle le service pour modifier ou publier l'offre.
        return service.modifier(id, demande, token);
    }


    // ========== 6. SUPPRIMER UNE OFFRE ==========

    // Permet de supprimer une offre avec une requête DELETE.
    @DeleteMapping("/{id}")

    // Retourne le code HTTP 204 si l'opération réussit.
    @ResponseStatus(HttpStatus.NO_CONTENT)

    // Demande la suppression d'une offre.
    public void supprimer(@PathVariable Long id, @RequestHeader(value = "Authorization", required = false) String token) {

        // Appelle le service pour vérifier les droits et supprimer l'offre.
        service.supprimer(id, token);
    }

}
