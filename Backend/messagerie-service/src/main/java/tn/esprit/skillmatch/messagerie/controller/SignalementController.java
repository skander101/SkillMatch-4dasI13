package tn.esprit.skillmatch.messagerie.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.skillmatch.messagerie.config.AuthenticatedUser;
import tn.esprit.skillmatch.messagerie.dto.CreateSignalementRequest;
import tn.esprit.skillmatch.messagerie.dto.SignalementResponse;
import tn.esprit.skillmatch.messagerie.dto.TraiterSignalementRequest;
import tn.esprit.skillmatch.messagerie.service.SignalementService;

import java.util.List;

@RestController
@RequestMapping("/skillmatch/messagerie")
@Tag(name = "Signalements", description = "Signalement de messages et traitement par l'administrateur")
public class SignalementController {

    private final SignalementService signalementService;

    public SignalementController(SignalementService signalementService) {
        this.signalementService = signalementService;
    }

    @PostMapping("/messages/{messageId}/signalements")
    @Operation(summary = "Signaler un message",
            description = "Permet à un participant de signaler un message avec un motif et un commentaire facultatif.")
    public ResponseEntity<SignalementResponse> signalerMessage(
            @PathVariable String messageId,
            @Valid @RequestBody CreateSignalementRequest request,
            @Parameter(hidden = true) AuthenticatedUser user) {
        SignalementResponse response = signalementService.signalerMessage(messageId, request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/admin/signalements")
    @Operation(summary = "Lister les signalements (ADMIN)",
            description = "Liste les signalements pour le back office admin.")
    public ResponseEntity<List<SignalementResponse>> listAdminSignalements(
            @RequestParam(required = false) String statut,
            @Parameter(hidden = true) AuthenticatedUser user) {
        return ResponseEntity.ok(signalementService.listForAdmin(statut, user));
    }

    @PostMapping("/admin/signalements/{id}/consulter")
    @Operation(summary = "Consulter un message signalé (ADMIN)",
            description = "Retourne le contenu du message signalé et enregistre la consultation dans le journal d'audit.")
    public ResponseEntity<SignalementResponse> consulterMessageSignale(
            @PathVariable String id,
            @Parameter(hidden = true) AuthenticatedUser user) {
        return ResponseEntity.ok(signalementService.consulterMessageSignale(id, user));
    }

    @PatchMapping("/admin/signalements/{id}/traiter")
    @Operation(summary = "Traiter un signalement (ADMIN)",
            description = "Applique une décision : IGNORER, AVERTIR, MASQUER_MESSAGE, SUSPENDRE_ENVOI.")
    public ResponseEntity<SignalementResponse> traiterSignalement(
            @PathVariable String id,
            @Valid @RequestBody TraiterSignalementRequest request,
            @Parameter(hidden = true) AuthenticatedUser user) {
        return ResponseEntity.ok(signalementService.traiterSignalement(id, request, user));
    }
}
