package tn.esprit.skillmatch.messagerie.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.skillmatch.messagerie.config.AuthenticatedUser;
import tn.esprit.skillmatch.messagerie.dto.AdminConversationDetailResponse;
import tn.esprit.skillmatch.messagerie.dto.AdminConversationSummaryResponse;
import tn.esprit.skillmatch.messagerie.dto.ConversationModerationRequest;
import tn.esprit.skillmatch.messagerie.dto.MessagerieAdminDashboardResponse;
import tn.esprit.skillmatch.messagerie.service.ConversationService;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/skillmatch/messagerie/admin")
@Tag(name = "Administration messagerie", description = "Supervision des conversations pour le backoffice")
public class ConversationAdminController {

    private final ConversationService conversationService;

    public ConversationAdminController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @GetMapping("/supervision")
    @Operation(summary = "Tableau de supervision de la messagerie",
            description = "Retourne les KPI et la liste filtrable et paginée des conversations pour l'interface d'administration.")
    public ResponseEntity<MessagerieAdminDashboardResponse> getDashboard(
            @RequestParam(required = false) String scope,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @Parameter(hidden = true) AuthenticatedUser user) {
        if (type == null && statut == null && page == null && size == null) {
            return ResponseEntity.ok(conversationService.getAdminDashboard(scope, query, user));
        }
        if (page == null && size == null) {
            return ResponseEntity.ok(conversationService.getAdminDashboard(scope, query, type, statut, user));
        }
        return ResponseEntity.ok(conversationService.getAdminDashboard(scope, query, type, statut, page, size, user));
    }

    @GetMapping(value = "/supervision/export", produces = "text/csv")
    @Operation(summary = "Exporter la supervision en CSV",
            description = "Exporte la liste filtrée des conversations de supervision au format CSV.")
    public ResponseEntity<String> exportDashboard(
            @RequestParam(required = false) String scope,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String statut,
            @Parameter(hidden = true) AuthenticatedUser user) {
        String csv = (type == null && statut == null)
                ? conversationService.exportAdminDashboardCsv(scope, query, user)
                : conversationService.exportAdminDashboardCsv(scope, query, type, statut, user);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"messagerie-supervision.csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv);
    }

    @GetMapping("/conversations/{id}")
    @Operation(summary = "Consulter les détails d'une conversation",
            description = "Retourne les métadonnées, les participants et les messages d'une conversation pour la supervision.")
    public ResponseEntity<AdminConversationDetailResponse> getConversationDetail(
            @PathVariable String id,
            @Parameter(hidden = true) AuthenticatedUser user) {
        return ResponseEntity.ok(conversationService.getAdminConversationDetail(id, user));
    }

    @GetMapping(value = "/conversations/{id}/export", produces = "text/csv")
    @Operation(summary = "Exporter une conversation en CSV",
            description = "Exporte les informations d'une conversation au format CSV et journalise l'export.")
    public ResponseEntity<String> exportSingleConversation(
            @PathVariable String id,
            @Parameter(hidden = true) AuthenticatedUser user) {
        String csv = conversationService.exportSingleConversationCsv(id, user);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"conversation-" + id + ".csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv);
    }

    @PatchMapping("/conversations/{id}/moderation")
    @Operation(summary = "Mettre à jour le statut de modération d'une conversation",
            description = "Permet à un administrateur de signaler, désignaler, archiver ou désarchiver une conversation.")
    public ResponseEntity<AdminConversationSummaryResponse> updateModeration(
            @PathVariable String id,
            @RequestBody ConversationModerationRequest request,
            @Parameter(hidden = true) AuthenticatedUser user) {
        return ResponseEntity.ok(conversationService.updateModeration(id, request, user));
    }
}
