package tn.esprit.skillmatch.alertes.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.skillmatch.alertes.dto.FavoriRequest;
import tn.esprit.skillmatch.alertes.dto.FavoriResponse;
import tn.esprit.skillmatch.alertes.security.CurrentUser;
import tn.esprit.skillmatch.alertes.service.FavoriService;

import java.util.List;

@RestController
@RequestMapping("/skillmatch/favorites")
public class FavoriController {

    private final FavoriService favoriService;

    public FavoriController(FavoriService favoriService) {
        this.favoriService = favoriService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FavoriResponse create(@RequestAttribute(CurrentUser.REQUEST_ATTRIBUTE) CurrentUser user,
                                 @Valid @RequestBody FavoriRequest request) {
        return favoriService.create(user.id(), request.offerId());
    }

    @GetMapping("/me")
    public List<FavoriResponse> findMine(@RequestAttribute(CurrentUser.REQUEST_ATTRIBUTE) CurrentUser user) {
        return favoriService.findMine(user.id());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestAttribute(CurrentUser.REQUEST_ATTRIBUTE) CurrentUser user,
                       @PathVariable Long id) {
        favoriService.delete(user.id(), id);
    }
}
