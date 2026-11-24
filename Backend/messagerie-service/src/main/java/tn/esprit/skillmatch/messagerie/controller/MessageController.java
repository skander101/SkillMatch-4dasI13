package tn.esprit.skillmatch.messagerie.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.skillmatch.messagerie.config.AuthenticatedUser;
import tn.esprit.skillmatch.messagerie.dto.MessageResponse;
import tn.esprit.skillmatch.messagerie.dto.UpdateMessageRequest;
import tn.esprit.skillmatch.messagerie.service.MessageService;

@RestController
@RequestMapping("/skillmatch/messagerie/messages")
@Tag(name = "Messages", description = "Opérations sur les messages individuels")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'un message", description = "Récupère un message par son id. 403 si non participant de la conversation.")
    public ResponseEntity<MessageResponse> getById(
            @PathVariable String id,
            @Parameter(hidden = true) AuthenticatedUser user) {
        return ResponseEntity.ok(messageService.findById(id, user));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier un message", description = "Modifie le contenu d'un message. Seul l'expéditeur peut modifier.")
    public ResponseEntity<MessageResponse> update(
            @PathVariable String id,
            @Valid @RequestBody UpdateMessageRequest request,
            @Parameter(hidden = true) AuthenticatedUser user) {
        return ResponseEntity.ok(messageService.update(id, request, user));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un message", description = "Supprime un message. Seul l'expéditeur peut supprimer.")
    public ResponseEntity<Void> delete(
            @PathVariable String id,
            @Parameter(hidden = true) AuthenticatedUser user) {
        messageService.delete(id, user);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/lu")
    @Operation(summary = "Marquer comme lu", description = "Marque un message comme lu. Seul un destinataire (pas l'expéditeur) peut marquer.")
    public ResponseEntity<MessageResponse> markAsRead(
            @PathVariable String id,
            @Parameter(hidden = true) AuthenticatedUser user) {
        return ResponseEntity.ok(messageService.markAsRead(id, user));
    }
}
