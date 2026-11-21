package tn.esprit.skillmatch.commun.dto;

public record AuthResponse(
        String token,
        UserResponse user
) {
}
