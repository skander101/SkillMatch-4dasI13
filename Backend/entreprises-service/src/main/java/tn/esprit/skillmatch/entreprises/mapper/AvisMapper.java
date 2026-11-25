package tn.esprit.skillmatch.entreprises.mapper;

import org.springframework.stereotype.Component;
import tn.esprit.skillmatch.entreprises.client.UtilisateurInfo;
import tn.esprit.skillmatch.entreprises.dto.AvisRequest;
import tn.esprit.skillmatch.entreprises.dto.AvisResponse;
import tn.esprit.skillmatch.entreprises.entity.AvisEntreprise;
import tn.esprit.skillmatch.entreprises.entity.Entreprise;
import tn.esprit.skillmatch.entreprises.entity.StatutModeration;

@Component
public class AvisMapper {

    public AvisEntreprise toEntity(AvisRequest request, Entreprise entreprise, UtilisateurInfo utilisateur) {
        AvisEntreprise avis = new AvisEntreprise();
        avis.setEntreprise(entreprise);
        avis.setUtilisateurId(utilisateur.id());
        avis.setNomUtilisateur(utilisateur.nom().trim());
        avis.setNote(request.note());
        avis.setCommentaire(request.commentaire().trim());
        avis.setStatut(StatutModeration.EN_ATTENTE);
        return avis;
    }

    public AvisResponse toResponse(AvisEntreprise avis) {
        return new AvisResponse(
                avis.getId(),
                avis.getEntreprise().getId(),
                avis.getUtilisateurId(),
                avis.getNomUtilisateur(),
                avis.getNote(),
                avis.getCommentaire(),
                avis.getStatut(),
                avis.getDateCreation()
        );
    }
}
