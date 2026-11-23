package tn.esprit.skillmatch.entreprises.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.skillmatch.entreprises.entity.AvisEntreprise;
import tn.esprit.skillmatch.entreprises.entity.StatutModeration;

import java.util.List;

public interface AvisEntrepriseRepository extends JpaRepository<AvisEntreprise, Long> {

    boolean existsByEntreprise_IdAndUtilisateurId(Long entrepriseId, Long utilisateurId);

    List<AvisEntreprise> findByEntreprise_IdOrderByDateCreationDesc(Long entrepriseId);

    List<AvisEntreprise> findByEntreprise_IdAndStatut(Long entrepriseId, StatutModeration statut);
}
