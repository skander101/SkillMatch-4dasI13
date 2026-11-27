package tn.esprit.skillmatch.messagerie.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.skillmatch.messagerie.entity.SuspensionEnvoi;

import java.util.List;

public interface SuspensionEnvoiRepository extends JpaRepository<SuspensionEnvoi, String> {

    boolean existsByUserIdAndActiveTrue(Long userId);

    List<SuspensionEnvoi> findByUserIdAndActiveTrue(Long userId);
}
