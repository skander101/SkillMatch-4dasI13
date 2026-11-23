package tn.esprit.skillmatch.messagerie.config;

/**
 * Utilisateur authentifié extrait du JWT + appel /auth/me.
 * Placé comme principal dans le SecurityContext.
 */
public record AuthenticatedUser(
        Long id,
        String email,
        String role
) {
    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role);
    }
}
