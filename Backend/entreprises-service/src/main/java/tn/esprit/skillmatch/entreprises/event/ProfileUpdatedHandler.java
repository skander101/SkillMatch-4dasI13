package tn.esprit.skillmatch.entreprises.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tn.esprit.skillmatch.entreprises.entity.AvisEntreprise;
import tn.esprit.skillmatch.entreprises.repository.AvisEntrepriseRepository;

import java.util.List;

@Service
public class ProfileUpdatedHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProfileUpdatedHandler.class);

    private final AvisEntrepriseRepository avisEntrepriseRepository;
    private final ObjectMapper objectMapper;

    public ProfileUpdatedHandler(AvisEntrepriseRepository avisEntrepriseRepository, ObjectMapper objectMapper) {
        this.avisEntrepriseRepository = avisEntrepriseRepository;
        this.objectMapper = objectMapper;
    }

    public void handle(EvenementEntreprise event) {
        if (event == null || !EvenementEntreprise.PROFILE_UPDATED.equals(event.type())) {
            return;
        }

        EvenementEntreprise.ProfileUpdated profil;
        try {
            profil = objectMapper.convertValue(event.donnees(), EvenementEntreprise.ProfileUpdated.class);
        } catch (IllegalArgumentException exception) {
            LOGGER.warn("ProfileUpdated ignoré : payload invalide ({})", exception.getMessage());
            return;
        }

        if (profil == null || profil.utilisateurId() == null || !StringUtils.hasText(profil.nom())) {
            return;
        }

        List<AvisEntreprise> avis = avisEntrepriseRepository.findByUtilisateurId(profil.utilisateurId());
        if (avis.isEmpty()) {
            return;
        }

        String nom = profil.nom().trim();
        avis.forEach(avisEntreprise -> avisEntreprise.setNomUtilisateur(nom));
        avisEntrepriseRepository.saveAll(avis);
    }
}
