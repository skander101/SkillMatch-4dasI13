package tn.esprit.skillmatch.profils.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.skillmatch.profils.dto.CompletenessResponse;
import tn.esprit.skillmatch.profils.dto.ProfilRequest;
import tn.esprit.skillmatch.profils.dto.ProfilResponse;
import tn.esprit.skillmatch.profils.dto.ProfilUpdate;
import tn.esprit.skillmatch.profils.service.ProfilService;

@RestController
@RequestMapping("/skillmatch/profiles")
public class ProfilController {

    private final ProfilService profilService;

    public ProfilController(ProfilService profilService) {
        this.profilService = profilService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProfilResponse creer(@Valid @RequestBody ProfilRequest request) {
        return profilService.creer(request);
    }

    @GetMapping("/me")
    public ProfilResponse monProfil(@RequestAttribute("userId") Long userId) {
        return profilService.findByUserId(userId);
    }

    @PutMapping("/me")
    public ProfilResponse modifier(@RequestAttribute("userId") Long userId,
                                   @Valid @RequestBody ProfilUpdate update) {
        return profilService.modifier(userId, update);
    }

    @GetMapping("/me/completeness")
    public CompletenessResponse completude(@RequestAttribute("userId") Long userId) {
        return profilService.completude(userId);
    }

    @GetMapping("/{userId}")
    public ProfilResponse parUtilisateur(@RequestAttribute("role") String role, Long userId) {
        if (!"RECRUTEUR".equals(role) && !"ADMIN".equals(role)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Accès réservé aux recruteurs");
        }
        return profilService.findByUserId(userId);
    }
}
