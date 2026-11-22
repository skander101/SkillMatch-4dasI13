package tn.esprit.skillmatch.offres.repository; // Accès aux candidatures.

import org.springframework.data.jpa.repository.JpaRepository; // Opérations JPA standard.
import tn.esprit.skillmatch.offres.entity.Candidature; // Entité candidature.
import java.util.List; // Listes accessibles à chaque rôle.

public interface CandidatureRepository extends JpaRepository<Candidature, Long> { // CRUD et recherches par propriétaire.
    boolean existsByOffreId(Long offreId); // Vérifie si une offre possède des candidatures.
    boolean existsByUserIdAndOffreId(Long userId, Long offreId); // Interdit de postuler deux fois.
    List<Candidature> findByUserId(Long userId); // Candidatures du candidat connecté.
    List<Candidature> findByOffreRecruteurId(Long recruteurId); // Candidatures reçues par le recruteur.
} // Fin du repository.
