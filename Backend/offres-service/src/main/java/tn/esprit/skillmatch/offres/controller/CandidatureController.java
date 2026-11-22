package tn.esprit.skillmatch.offres.controller; // Routes REST des candidatures.

import jakarta.validation.Valid; // Valide les formulaires.
import org.springframework.http.HttpStatus; // Codes de succès.
import org.springframework.web.bind.annotation.*; // Annotations REST.
import tn.esprit.skillmatch.offres.dto.*; // Formulaires et réponse JSON.
import tn.esprit.skillmatch.offres.service.CandidatureService; // Règles métier.
import java.util.List; // Listes de réponses.

@RestController // Réponses JSON.
@RequestMapping("/skillmatch/candidatures") // Préfixe du CRUD.
public class CandidatureController {
    private final CandidatureService service; // Service partagé par les rôles.

    public CandidatureController(CandidatureService service) { // Injection par constructeur.
        this.service = service;
    }

    @GetMapping // Liste selon le rôle.
    public List<CandidatureResponse> lister(@RequestParam(defaultValue = "") String search, @RequestParam(defaultValue = "dateCandidatureDesc") String sort, // Deux paramètres de l'URL.
                                           @RequestHeader(value = "Authorization", required = false) String token) {
        return service.lister(token, search, sort); // Récentes d'abord par défaut.
    }

    @GetMapping("/{id}") // Détail d'une candidature autorisée.
    public CandidatureResponse consulter(@PathVariable Long id, @RequestHeader(value = "Authorization", required = false) String token) {
        return service.consulter(id, token);
    }

    @PostMapping // Crée une candidature RECUE.
    @ResponseStatus(HttpStatus.CREATED) // Réponse 201.
    public CandidatureResponse creer(@Valid @RequestBody CandidatureRequest demande, @RequestHeader(value = "Authorization", required = false) String token) {
        return service.creer(demande, token);
    }

    @PutMapping("/{id}") // Modification selon le rôle.
    public CandidatureResponse modifier(@PathVariable Long id, @Valid @RequestBody CandidatureUpdate demande, @RequestHeader(value = "Authorization", required = false) String token) {
        return service.modifier(id, demande, token);
    }

    @DeleteMapping("/{id}") // Retrait candidat ou suppression ADMIN.
    @ResponseStatus(HttpStatus.NO_CONTENT) // Réponse 204.
    public void supprimer(@PathVariable Long id, @RequestHeader(value = "Authorization", required = false) String token) {
        service.supprimer(id, token);
    }
}
