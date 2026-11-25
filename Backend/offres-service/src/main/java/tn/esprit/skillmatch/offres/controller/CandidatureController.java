
package tn.esprit.skillmatch.offres.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import tn.esprit.skillmatch.offres.dto.*;
import tn.esprit.skillmatch.offres.service.CandidatureService;
import java.util.List;


// ========== CONTROLLER DES CANDIDATURES ==========

// Indique que cette classe gère les requêtes REST et retourne du JSON.
@RestController

// Définit l'URL principale des candidatures.
@RequestMapping("/skillmatch/candidatures")

// Classe qui gère les opérations CRUD des candidatures.
public class CandidatureController {


    // ========== DÉCLARATION DU SERVICE ==========

    // Déclare le service qui contient les règles métier.
    private final CandidatureService service;


    // ========== CONSTRUCTEUR ==========

    // Injecte automatiquement le service dans le contrôleur.
    public CandidatureController(CandidatureService service) {

        // Initialise le service utilisé par le contrôleur.
        this.service = service;
    }


    // ========== 1. LISTER LES CANDIDATURES ==========

    // Permet de récupérer les candidatures avec une requête GET.
    @GetMapping

    // Récupère la liste des candidatures avec recherche et tri.
    public List<CandidatureResponse> lister(@RequestParam(defaultValue = "") String search, @RequestParam(defaultValue = "dateCandidatureDesc") String sort,
                                            @RequestHeader(value = "Authorization", required = false) String token) {

        // Appelle le service pour retourner les candidatures autorisées.
        return service.lister(token, search, sort);
    }


    // ========== 2. CONSULTER UNE CANDIDATURE ==========

    // Permet de récupérer une candidature par son identifiant.
    @GetMapping("/{id}")

    // Récupère les détails d'une candidature.
    public CandidatureResponse consulter(@PathVariable Long id, @RequestHeader(value = "Authorization", required = false) String token) {

        // Appelle le service pour consulter la candidature demandée.
        return service.consulter(id, token);
    }


    // ========== 3. CRÉER UNE CANDIDATURE ==========

    // Permet de créer une candidature avec une requête POST.
    @PostMapping

    // Retourne le code HTTP 201 si la création réussit.
    @ResponseStatus(HttpStatus.CREATED)

    // Reçoit et valide les informations d'une nouvelle candidature.
    public CandidatureResponse creer(@Valid @RequestBody CandidatureRequest demande, @RequestHeader(value = "Authorization", required = false) String token) {

        // Appelle le service pour créer et retourner la candidature.
        return service.creer(demande, token);
    }


    // ========== 4. MODIFIER UNE CANDIDATURE ==========

    // Permet de modifier une candidature avec une requête PUT.
    @PutMapping("/{id}")

    // Reçoit et valide les informations à modifier.
    public CandidatureResponse modifier(@PathVariable Long id, @Valid @RequestBody CandidatureUpdate demande, @RequestHeader(value = "Authorization", required = false) String token) {

        // Appelle le service pour modifier la candidature.
        return service.modifier(id, demande, token);
    }


    // ========== 5. SUPPRIMER UNE CANDIDATURE ==========

    // Permet de demander la suppression avec une requête DELETE.
    @DeleteMapping("/{id}")

    // Retourne le code HTTP 204 si l'opération réussit.
    @ResponseStatus(HttpStatus.NO_CONTENT)

    // Demande le retrait ou la suppression d'une candidature.
    public void supprimer(@PathVariable Long id, @RequestHeader(value = "Authorization", required = false) String token) {

        // Appelle le service pour effectuer l'opération autorisée.
        service.supprimer(id, token);
    }

}
