package tn.esprit.skillmatch.offres.repository; // Accès aux offres en base.

import org.springframework.data.jpa.repository.JpaRepository; // Fournit les opérations CRUD.
import tn.esprit.skillmatch.offres.entity.Offre; // Entité enregistrée.
import tn.esprit.skillmatch.offres.enums.OffreStatut; // Statuts autorisés.
import java.time.LocalDate; // Date utilisée pour les offres ouvertes.
import java.util.List; // Résultats sous forme de liste.

public interface OffreRepository extends JpaRepository<Offre, Long> { // JPA implémente ces méthodes.
    List<Offre> findByRecruteurId(Long recruteurId); // Offres d'un seul propriétaire.
    List<Offre> findByStatutAndDateExpirationGreaterThanEqual(OffreStatut statut, LocalDate date); // Offres encore valides.
} // Fin du repository.
