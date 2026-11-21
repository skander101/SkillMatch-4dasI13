package tn.esprit.skillmatch.commun.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import tn.esprit.skillmatch.commun.entity.Role;

public record UpdateUtilisateurRequest(
        @NotBlank String nom,
        @NotBlank @Email String email,
        @NotNull Role role,
        String password
) {
}
