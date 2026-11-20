package tn.esprit.skillmatch.offres.repository; // Accès aux candidatures.

import org.springframework.data.jpa.repository.JpaRepository; // Opérations JPA standard.
import tn.esprit.skillmatch.offres.entity.Candidature; // Entité candidature.

public interface CandidatureRepository extends JpaRepository<Candidature, Long> { // Pas encore de contrôleur candidature.
    boolean existsByOffreId(Long offreId); // Vérifie si une offre possède des candidatures.
} // Fin du repository.
