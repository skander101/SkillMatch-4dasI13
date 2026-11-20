package tn.esprit.skillmatch.commun.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import tn.esprit.skillmatch.commun.entity.Role;

public record RegisterRequest(
        @NotBlank String nom,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 6) String password,
        @NotNull Role role
) {
}
