package tn.esprit.skillmatch.commun.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.skillmatch.commun.dto.CreateUtilisateurRequest;
import tn.esprit.skillmatch.commun.dto.UpdateUtilisateurRequest;
import tn.esprit.skillmatch.commun.dto.UserResponse;
import tn.esprit.skillmatch.commun.service.UtilisateurService;

import java.util.List;

@RestController
@RequestMapping("/skillmatch/utilisateurs")
public class UtilisateurController {

    private final UtilisateurService utilisateurService;

    public UtilisateurController(UtilisateurService utilisateurService) {
        this.utilisateurService = utilisateurService;
    }

    @GetMapping
    public List<UserResponse> findAll() {
        return utilisateurService.findAll();
    }

    @GetMapping("/{id}")
    public UserResponse findById(@PathVariable Long id) {
        return utilisateurService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody CreateUtilisateurRequest request) {
        return utilisateurService.create(request);
    }

    @PutMapping("/{id}")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UpdateUtilisateurRequest request) {
        return utilisateurService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        utilisateurService.delete(id);
    }
}
