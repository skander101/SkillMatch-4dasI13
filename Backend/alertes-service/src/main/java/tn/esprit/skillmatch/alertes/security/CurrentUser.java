package tn.esprit.skillmatch.alertes.security;

/** Utilisateur connecté, déduit du JWT. Placé dans la requête par CandidatInterceptor. */
public record CurrentUser(
        Long id,
        String email,
        String role
) {
    public static final String REQUEST_ATTRIBUTE = "currentUser";
}
