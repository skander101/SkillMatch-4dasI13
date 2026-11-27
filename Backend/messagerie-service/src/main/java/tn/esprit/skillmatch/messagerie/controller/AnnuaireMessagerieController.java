package tn.esprit.skillmatch.messagerie.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.skillmatch.messagerie.config.AuthenticatedUser;
import tn.esprit.skillmatch.messagerie.dto.ParticipantSummary;
import tn.esprit.skillmatch.messagerie.service.ConversationService;

import java.util.List;

@RestController
@RequestMapping("/skillmatch/messagerie/utilisateurs")
@Tag(name = "Annuaire messagerie", description = "Recherche de personnes par nom via UserClient")
public class AnnuaireMessagerieController {

    private final ConversationService conversationService;

    public AnnuaireMessagerieController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @GetMapping("/recherche")
    @Operation(summary = "Rechercher des personnes par nom",
            description = "Recherche insensible aux accents et à la casse (10 résultats max), ou contacts récents si vide. Masque les admins aux non-admins.")
    public ResponseEntity<List<ParticipantSummary>> search(
            @RequestParam(name = "q", required = false, defaultValue = "") String q,
            @RequestParam(name = "role", required = false) String role,
            @Parameter(hidden = true) AuthenticatedUser user) {
        return ResponseEntity.ok(conversationService.searchUsers(q, role, user));
    }
}
