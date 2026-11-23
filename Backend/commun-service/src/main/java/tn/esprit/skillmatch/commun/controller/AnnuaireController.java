package tn.esprit.skillmatch.commun.controller;

import io.jsonwebtoken.JwtException;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.skillmatch.commun.dto.AnnuaireBatchRequest;
import tn.esprit.skillmatch.commun.dto.AnnuaireUserResponse;
import tn.esprit.skillmatch.commun.entity.Role;
import tn.esprit.skillmatch.commun.entity.Utilisateur;
import tn.esprit.skillmatch.commun.repository.UtilisateurRepository;
import tn.esprit.skillmatch.commun.security.JwtService;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Annuaire en lecture seule : recherche de destinataires par nom.
 * Toutes les routes sont protégées par un JWT valide.
 * Seuls id, nom et role sont exposés (jamais l'email ni le mot de passe).
 *
 * Aucun fichier existant de commun-service n'est modifié : le filtrage se fait
 * en mémoire à partir de findAll(), comme précisé dans la mission.
 */
@RestController
@RequestMapping("/skillmatch/annuaire")
public class AnnuaireController {

    private static final int MIN_QUERY_LENGTH = 2;
    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 10;

    private final UtilisateurRepository utilisateurRepository;
    private final JwtService jwtService;

    public AnnuaireController(UtilisateurRepository utilisateurRepository, JwtService jwtService) {
        this.utilisateurRepository = utilisateurRepository;
        this.jwtService = jwtService;
    }

    /**
     * GET /skillmatch/annuaire/utilisateurs?q=...&role=...&limit=10
     * Recherche par nom, insensible à la casse et aux accents,
     * minimum 2 caractères, 10 résultats maximum, utilisateur connecté exclu.
     */
    @GetMapping("/utilisateurs")
    public ResponseEntity<List<AnnuaireUserResponse>> search(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "role", required = false) String role,
            @RequestParam(name = "limit", required = false, defaultValue = "" + DEFAULT_LIMIT) int limit,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {

        Utilisateur current = requireCurrentUser(authorization);

        String query = normalize(q);
        if (query.length() < MIN_QUERY_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La recherche doit contenir au moins " + MIN_QUERY_LENGTH + " caractères");
        }

        Role roleFilter = parseRole(role);
        int effectiveLimit = limit < 1 ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);

        List<AnnuaireUserResponse> results = utilisateurRepository.findAll().stream()
                .filter(user -> !Objects.equals(user.getId(), current.getId()))
                .filter(user -> roleFilter == null || user.getRole() == roleFilter)
                .filter(user -> normalize(user.getNom()).contains(query))
                .sorted(Comparator.comparing(user -> normalize(user.getNom())))
                .limit(effectiveLimit)
                .map(this::toPublic)
                .toList();

        return ResponseEntity.ok(results);
    }

    /**
     * GET /skillmatch/annuaire/utilisateurs/{id}
     */
    @GetMapping("/utilisateurs/{id}")
    public ResponseEntity<AnnuaireUserResponse> findById(
            @PathVariable Long id,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {

        requireCurrentUser(authorization);

        Utilisateur user = utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));

        return ResponseEntity.ok(toPublic(user));
    }

    /**
     * POST /skillmatch/annuaire/utilisateurs/batch
     * Résout une liste d'identifiants. Les ids inconnus sont simplement absents
     * de la réponse (aucune fuite d'information).
     */
    @PostMapping("/utilisateurs/batch")
    public ResponseEntity<List<AnnuaireUserResponse>> batch(
            @Valid @RequestBody AnnuaireBatchRequest request,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {

        requireCurrentUser(authorization);

        Set<Long> ids = new LinkedHashSet<>();
        if (request.ids() != null) {
            request.ids().stream().filter(Objects::nonNull).forEach(ids::add);
        }

        List<AnnuaireUserResponse> results = utilisateurRepository.findAll().stream()
                .filter(user -> ids.contains(user.getId()))
                .sorted(Comparator.comparing(user -> ids.stream().toList().indexOf(user.getId())))
                .map(this::toPublic)
                .toList();

        return ResponseEntity.ok(results);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** Valide le JWT et renvoie l'utilisateur connecté. 401 si absent ou invalide. */
    private Utilisateur requireCurrentUser(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ") || authorization.length() <= 7) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentification requise");
        }

        String token = authorization.substring(7);
        String email;
        try {
            email = jwtService.extractEmail(token);
        } catch (JwtException | IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token JWT invalide ou expiré");
        }

        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token JWT invalide ou expiré");
        }

        return utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token JWT invalide ou expiré"));
    }

    private Role parseRole(String role) {
        if (role == null || role.isBlank()) {
            return null;
        }
        try {
            return Role.valueOf(role.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rôle invalide : " + role);
        }
    }

    /** Minuscules + suppression des accents et des diacritiques. */
    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String stripped = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return stripped.toLowerCase(Locale.ROOT).trim();
    }

    /** Ne projette que id, nom et role. */
    private AnnuaireUserResponse toPublic(Utilisateur user) {
        return new AnnuaireUserResponse(user.getId(), user.getNom(), user.getRole());
    }
}
