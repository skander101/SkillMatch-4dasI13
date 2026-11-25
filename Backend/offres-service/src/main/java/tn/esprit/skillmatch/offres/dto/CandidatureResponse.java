package tn.esprit.skillmatch.offres.dto; // Réponse JSON sans objet JPA imbriqué.

import tn.esprit.skillmatch.offres.entity.Candidature; // Entité à convertir.
import tn.esprit.skillmatch.offres.enums.CandidatureStatut; // Statut retourné.
import java.time.LocalDateTime; // Dates retournées.

//permet de regrouper les informations d’une candidature pour les envoyer au frontend Angular sous forme de JSON.
public record CandidatureResponse( // Utilise offreId au lieu de sérialiser la relation JPA.
        Long id, Long offreId, CandidatureStatut statut, String lettre, // Données principales.
        LocalDateTime dateCandidature, LocalDateTime dateEntretien, // Dates.
        Integer noteRecruteur, String commentaireRecruteur, Long userId, Long cvId, String offreTitre // Titre lisible sur les cartes.
) {

    //permet de transformer une entité Candidature récupérée depuis MySQL en un objet CandidatureResponse.
    public static CandidatureResponse depuis(Candidature c) { // Conversion explicite et courte.
        return new CandidatureResponse(c.getId(), c.getOffre().getId(), c.getStatut(), c.getLettre(),
                c.getDateCandidature(), c.getDateEntretien(), c.getNoteRecruteur(), // Dates et note.
                c.getCommentaireRecruteur(), c.getUserId(), c.getCvId(), c.getOffre().getTitre());
    }
}
