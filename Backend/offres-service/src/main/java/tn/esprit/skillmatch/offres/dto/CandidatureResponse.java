package tn.esprit.skillmatch.offres.dto; // Réponse JSON sans objet JPA imbriqué.

import tn.esprit.skillmatch.offres.entity.Candidature; // Entité à convertir.
import tn.esprit.skillmatch.offres.enums.CandidatureStatut; // Statut retourné.
import java.time.LocalDateTime; // Dates retournées.

public record CandidatureResponse( // Utilise offreId au lieu de sérialiser la relation JPA.
        Long id, Long offreId, CandidatureStatut statut, String lettre, // Données principales.
        LocalDateTime dateCandidature, LocalDateTime dateEntretien, // Dates.
        Integer noteRecruteur, String commentaireRecruteur, Long userId, Long cvId, String offreTitre // Titre lisible sur les cartes.
) {
    public static CandidatureResponse depuis(Candidature c) { // Conversion explicite et courte.
        return new CandidatureResponse(c.getId(), c.getOffre().getId(), c.getStatut(), c.getLettre(), // Champs principaux.
                c.getDateCandidature(), c.getDateEntretien(), c.getNoteRecruteur(), // Dates et note.
                c.getCommentaireRecruteur(), c.getUserId(), c.getCvId(), c.getOffre().getTitre()); // Le titre vient de la relation existante.
    }
}
