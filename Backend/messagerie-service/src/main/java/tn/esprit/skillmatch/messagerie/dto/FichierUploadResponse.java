package tn.esprit.skillmatch.messagerie.dto;

public record FichierUploadResponse(
        String url,
        String nom,
        long taille
) {}
