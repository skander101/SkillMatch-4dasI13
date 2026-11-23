package tn.esprit.skillmatch.messagerie.dto;

/**
 * Miroir de la réponse de GET /skillmatch/auth/me (commun-service).
 * Contient les champs retournés par UserResponse(Long id, String nom, String email, Role role).
 */
public record MeResponse(
        Long id,
        String nom,
        String email,
        String role
) {}
