package tn.esprit.skillmatch.commun.dto;

import tn.esprit.skillmatch.commun.entity.Role;

public record UserResponse(
        Long id,
        String nom,
        String email,
        Role role
) {
}
