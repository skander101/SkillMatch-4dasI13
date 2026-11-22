package tn.esprit.skillmatch.messagerie.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateMessageRequest(
        @NotBlank(message = "Le contenu du message ne peut pas être vide")
        String contenu
) {}
