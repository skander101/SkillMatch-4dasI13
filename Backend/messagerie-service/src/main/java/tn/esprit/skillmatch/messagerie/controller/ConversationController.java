package tn.esprit.skillmatch.messagerie.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.skillmatch.messagerie.config.AuthenticatedUser;
import tn.esprit.skillmatch.messagerie.dto.ConversationResponse;
import tn.esprit.skillmatch.messagerie.dto.CreateConversationRequest;
import tn.esprit.skillmatch.messagerie.dto.FichierUploadResponse;
import tn.esprit.skillmatch.messagerie.dto.MessageResponse;
import tn.esprit.skillmatch.messagerie.dto.SendMessageRequest;
import tn.esprit.skillmatch.messagerie.service.ConversationService;
import tn.esprit.skillmatch.messagerie.service.MessageService;

import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/skillmatch/messagerie/conversations")
@Tag(name = "Conversations", description = "Gestion des conversations et groupes entre candidats et recruteurs")
public class ConversationController {

    private final ConversationService conversationService;
    private final MessageService messageService;

    public ConversationController(ConversationService conversationService, MessageService messageService) {
        this.conversationService = conversationService;
        this.messageService = messageService;
    }

    @PostMapping
    @Operation(summary = "Créer une conversation ou un groupe",
            description = "Crée une conversation directe (2 personnes) ou un groupe (3+ personnes). " +
                    "Le créateur doit figurer dans les participants. " +
                    "Un Candidat ou Recruteur ne peut pas cibler un Admin. 409 si elle existe déjà.")
    public ResponseEntity<ConversationResponse> create(
            @Valid @RequestBody CreateConversationRequest request,
            @Parameter(hidden = true) AuthenticatedUser user) {
        ConversationResponse response = conversationService.create(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Mes conversations", description = "Liste les conversations de l'utilisateur authentifié.")
    public ResponseEntity<List<ConversationResponse>> getMyConversations(
            @Parameter(hidden = true) AuthenticatedUser user) {
        return ResponseEntity.ok(conversationService.findByUser(user));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'une conversation", description = "Récupère une conversation par son id. 403 si non participant.")
    public ResponseEntity<ConversationResponse> getById(
            @PathVariable String id,
            @Parameter(hidden = true) AuthenticatedUser user) {
        return ResponseEntity.ok(conversationService.findById(id, user));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier les participants",
            description = "Remplace les participants. Seul le créateur ou ADMIN peut modifier. " +
                    "Le créateur doit rester participant. Un Candidat/Recruteur ne peut pas ajouter un Admin.")
    public ResponseEntity<ConversationResponse> update(
            @PathVariable String id,
            @Valid @RequestBody CreateConversationRequest request,
            @Parameter(hidden = true) AuthenticatedUser user) {
        return ResponseEntity.ok(conversationService.updateParticipants(id, request, user));
    }

    @PostMapping("/{id}/quitter")
    @Operation(summary = "Quitter un groupe",
            description = "Permet à un membre (y compris le créateur s'il reste au moins 2 membres) de quitter un groupe. 400 sur conversation directe.")
    public ResponseEntity<ConversationResponse> quitterGroupe(
            @PathVariable String id,
            @Parameter(hidden = true) AuthenticatedUser user) {
        return ResponseEntity.ok(conversationService.quitterGroupe(id, user));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une conversation", description = "Supprime la conversation et ses messages. Seul le créateur ou ADMIN peut supprimer.")
    public ResponseEntity<Void> delete(
            @PathVariable String id,
            @Parameter(hidden = true) AuthenticatedUser user) {
        conversationService.delete(id, user);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/fichiers")
    @Operation(summary = "Joindre un fichier",
            description = "Joigne un fichier (pdf, jpg, png, doc, docx ; 10 Mo max) à une conversation. Seul un participant peut joindre un fichier.")
    public ResponseEntity<FichierUploadResponse> uploadFichier(
            @PathVariable String id,
            @RequestParam("fichier") MultipartFile fichier,
            @Parameter(hidden = true) AuthenticatedUser user) {
        FichierUploadResponse response = conversationService.uploadFichier(id, fichier, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}/fichiers/{filename:.+}")
    @Operation(summary = "Télécharger un fichier joint",
            description = "Télécharge un fichier joint protégé par JWT pour les participants de la conversation.")
    public ResponseEntity<Resource> downloadFichier(
            @PathVariable String id,
            @PathVariable String filename,
            @Parameter(hidden = true) AuthenticatedUser user) {
        Resource resource = conversationService.loadFichier(id, filename, user);
        MediaType mediaType = resolveMediaType(filename);
        String disposition = (mediaType.equals(MediaType.APPLICATION_PDF)
                || "image".equals(mediaType.getType())) ? "inline" : "attachment";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition + "; filename=\"" + resource.getFilename() + "\"")
                .header("X-Content-Type-Options", "nosniff")
                .contentType(mediaType)
                .body(resource);
    }

    @GetMapping("/{id}/messages")
    @Operation(summary = "Messages d'une conversation", description = "Liste les messages d'une conversation, triés par date croissante.")
    public ResponseEntity<List<MessageResponse>> getMessages(
            @PathVariable String id,
            @Parameter(hidden = true) AuthenticatedUser user) {
        return ResponseEntity.ok(messageService.listByConversation(id, user));
    }

    @PostMapping("/{id}/messages")
    @Operation(summary = "Envoyer un message", description = "Envoie un message dans une conversation. " +
            "Un Candidat ou Recruteur ne peut pas envoyer dans une conversation contenant un Admin.")
    public ResponseEntity<MessageResponse> sendMessage(
            @PathVariable String id,
            @Valid @RequestBody SendMessageRequest request,
            @Parameter(hidden = true) AuthenticatedUser user) {
        MessageResponse response = messageService.send(id, request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private MediaType resolveMediaType(String filename) {
        String lower = filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".pdf")) return MediaType.APPLICATION_PDF;
        if (lower.endsWith(".png")) return MediaType.IMAGE_PNG;
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return MediaType.IMAGE_JPEG;
        if (lower.endsWith(".doc")) return MediaType.parseMediaType("application/msword");
        if (lower.endsWith(".docx")) {
            return MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
