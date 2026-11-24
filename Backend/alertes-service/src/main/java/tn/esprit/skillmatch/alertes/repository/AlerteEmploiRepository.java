package tn.esprit.skillmatch.alertes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.skillmatch.alertes.entity.AlerteEmploi;

import java.util.List;

public interface AlerteEmploiRepository extends JpaRepository<AlerteEmploi, Long> {

    List<AlerteEmploi> findByUserIdOrderByDateCreationDesc(Long userId);

    List<AlerteEmploi> findByUserId(Long userId);

    List<AlerteEmploi> findByActiveTrue();

    long countByUserIdAndActiveTrue(Long userId);
}
