package tn.esprit.skillmatch.alertes.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.skillmatch.alertes.dto.ActiveRequest;
import tn.esprit.skillmatch.alertes.dto.AlerteResponse;
import tn.esprit.skillmatch.alertes.dto.FavoriResponse;
import tn.esprit.skillmatch.alertes.service.AlerteService;
import tn.esprit.skillmatch.alertes.service.FavoriService;

import java.util.List;

/** Endpoints du back office « Alertes & Favoris » (rôle ADMIN, contrôlé par AdminInterceptor). */
@RestController
public class AdminController {

    private final AlerteService alerteService;
    private final FavoriService favoriService;

    public AdminController(AlerteService alerteService, FavoriService favoriService) {
        this.alerteService = alerteService;
        this.favoriService = favoriService;
    }

    @GetMapping("/skillmatch/alerts/admin")
    public List<AlerteResponse> findAllAlertes() {
        return alerteService.findAll();
    }

    @PutMapping("/skillmatch/alerts/admin/{id}/active")
    public AlerteResponse changeActive(@PathVariable Long id, @Valid @RequestBody ActiveRequest request) {
        return alerteService.adminChangeActive(id, request.active());
    }

    @DeleteMapping("/skillmatch/alerts/admin/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAlerte(@PathVariable Long id) {
        alerteService.adminDelete(id);
    }

    @GetMapping("/skillmatch/favorites/admin")
    public List<FavoriResponse> findAllFavoris() {
        return favoriService.findAll();
    }

    @DeleteMapping("/skillmatch/favorites/admin/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFavori(@PathVariable Long id) {
        favoriService.adminDelete(id);
    }
}
