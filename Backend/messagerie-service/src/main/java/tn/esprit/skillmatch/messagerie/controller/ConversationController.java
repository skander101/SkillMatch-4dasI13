package tn.esprit.skillmatch.messagerie.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.skillmatch.messagerie.config.AuthenticatedUser;
import tn.esprit.skillmatch.messagerie.dto.ConversationResponse;
import tn.esprit.skillmatch.messagerie.dto.CreateConversationRequest;
import tn.esprit.skillmatch.messagerie.dto.MessageResponse;
import tn.esprit.skillmatch.messagerie.dto.SendMessageRequest;
import tn.esprit.skillmatch.messagerie.service.ConversationService;
import tn.esprit.skillmatch.messagerie.service.MessageService;

import java.util.List;

@RestController
@RequestMapping("/skillmatch/messagerie/conversations")
@Tag(name = "Conversations", description = "Gestion des conversations entre candidats et recruteurs")
public class ConversationController {

    private final ConversationService conversationService;
    private final MessageService messageService;

    public ConversationController(ConversationService conversationService, MessageService messageService) {
        this.conversationService = conversationService;
        this.messageService = messageService;
    }

    @PostMapping
    @Operation(summary = "Créer une conversation", description = "Crée une conversation entre les participants. 409 si elle existe déjà.")
    public ResponseEntity<ConversationResponse> create(
            @Valid @RequestBody CreateConversationRequest request,
            AuthenticatedUser user) {
        ConversationResponse response = conversationService.create(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Mes conversations", description = "Liste les conversations de l'utilisateur authentifié. Un ADMIN voit toutes les conversations.")
    public ResponseEntity<List<ConversationResponse>> getMyConversations(
            AuthenticatedUser user) {
        return ResponseEntity.ok(conversationService.findByUser(user));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'une conversation", description = "Récupère une conversation par son id. 403 si non participant.")
    public ResponseEntity<ConversationResponse> getById(
            @PathVariable String id,
            AuthenticatedUser user) {
        return ResponseEntity.ok(conversationService.findById(id, user));
    }

    @GetMapping("/{id}/messages")
    @Operation(summary = "Messages d'une conversation", description = "Liste les messages d'une conversation, triés par date croissante.")
    public ResponseEntity<List<MessageResponse>> getMessages(
            @PathVariable String id,
            AuthenticatedUser user) {
        return ResponseEntity.ok(messageService.listByConversation(id, user));
    }

    @PostMapping("/{id}/messages")
    @Operation(summary = "Envoyer un message", description = "Envoie un message dans une conversation. Le senderId vient du JWT.")
    public ResponseEntity<MessageResponse> sendMessage(
            @PathVariable String id,
            @Valid @RequestBody SendMessageRequest request,
            AuthenticatedUser user) {
        MessageResponse response = messageService.send(id, request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
