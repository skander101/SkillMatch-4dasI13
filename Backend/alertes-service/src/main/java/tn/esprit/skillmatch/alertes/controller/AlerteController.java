package tn.esprit.skillmatch.alertes.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
import tn.esprit.skillmatch.alertes.dto.ActiveRequest;
import tn.esprit.skillmatch.alertes.dto.AlerteRequest;
import tn.esprit.skillmatch.alertes.dto.AlerteResponse;
import tn.esprit.skillmatch.alertes.security.CurrentUser;
import tn.esprit.skillmatch.alertes.service.AlerteService;

import java.util.List;

@RestController
@RequestMapping("/skillmatch/alerts")
public class AlerteController {

    private final AlerteService alerteService;

    public AlerteController(AlerteService alerteService) {
        this.alerteService = alerteService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AlerteResponse create(@RequestAttribute(CurrentUser.REQUEST_ATTRIBUTE) CurrentUser user,
                                 @Valid @RequestBody AlerteRequest request) {
        return alerteService.create(user.id(), request);
    }

    @GetMapping("/me")
    public List<AlerteResponse> findMine(@RequestAttribute(CurrentUser.REQUEST_ATTRIBUTE) CurrentUser user) {
        return alerteService.findMine(user.id());
    }

    @GetMapping("/{id}")
    public AlerteResponse findById(@RequestAttribute(CurrentUser.REQUEST_ATTRIBUTE) CurrentUser user,
                                   @PathVariable Long id) {
        return alerteService.findById(user.id(), id);
    }

    @PutMapping("/{id}")
    public AlerteResponse update(@RequestAttribute(CurrentUser.REQUEST_ATTRIBUTE) CurrentUser user,
                                 @PathVariable Long id,
                                 @Valid @RequestBody AlerteRequest request) {
        return alerteService.update(user.id(), id, request);
    }

    @PutMapping("/{id}/active")
    public AlerteResponse changeActive(@RequestAttribute(CurrentUser.REQUEST_ATTRIBUTE) CurrentUser user,
                                       @PathVariable Long id,
                                       @Valid @RequestBody ActiveRequest request) {
        return alerteService.changeActive(user.id(), id, request.active());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestAttribute(CurrentUser.REQUEST_ATTRIBUTE) CurrentUser user,
                       @PathVariable Long id) {
        alerteService.delete(user.id(), id);
    }
}
