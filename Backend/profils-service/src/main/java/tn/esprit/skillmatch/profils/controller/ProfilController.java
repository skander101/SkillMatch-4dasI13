package tn.esprit.skillmatch.profils.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.skillmatch.profils.dto.CVResponse;
import tn.esprit.skillmatch.profils.dto.CompletenessResponse;
import tn.esprit.skillmatch.profils.dto.ProfilRequest;
import tn.esprit.skillmatch.profils.dto.ProfilResponse;
import tn.esprit.skillmatch.profils.dto.ProfilUpdate;
import tn.esprit.skillmatch.profils.service.ProfilService;

import java.util.List;

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

    @GetMapping
    public List<ProfilResponse> tous(@RequestAttribute("role") String role) {
        exigerRole(role);
        return profilService.listerProfils();
    }

    @GetMapping("/cvs")
    public List<CVResponse> tousLesCVs(@RequestAttribute("role") String role) {
        exigerRole(role);
        return profilService.listerCVs();
    }

    @DeleteMapping("/cv/{cvId}")
    public ResponseEntity<Void> supprimerCv(@RequestAttribute("role") String role, @PathVariable Long cvId) {
        exigerAdmin(role);
        profilService.supprimerCVParAdmin(cvId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> supprimer(@RequestAttribute("role") String role, @PathVariable Long userId) {
        exigerAdmin(role);
        profilService.supprimerProfilParAdmin(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{userId}")
    public ProfilResponse parUtilisateur(@RequestAttribute("role") String role, @PathVariable Long userId) {
        exigerRole(role);
        return profilService.findByUserId(userId);
    }

    private void exigerAdmin(String role) {
        if (!"ADMIN".equals(role)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Accès réservé aux administrateurs");
        }
    }

    private void exigerRole(String role) {
        if (!"RECRUTEUR".equals(role) && !"ADMIN".equals(role)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Accès réservé aux recruteurs et administrateurs");
        }
    }
}
