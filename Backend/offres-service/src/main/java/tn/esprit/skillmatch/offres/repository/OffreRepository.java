
package tn.esprit.skillmatch.offres.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.skillmatch.offres.entity.Offre;
import tn.esprit.skillmatch.offres.enums.OffreStatut;
import java.time.LocalDate;
import java.util.List;


// ========== REPOSITORY DES OFFRES ==========

// Interface qui permet d'accéder aux offres dans MySQL.
// JpaRepository fournit automatiquement les opérations CRUD.
public interface OffreRepository extends JpaRepository<Offre, Long> {


    // ========== 1. OFFRES D'UN RECRUTEUR ==========

    // Récupère toutes les offres appartenant à un recruteur.
    List<Offre> findByRecruteurId(Long recruteurId);


    // ========== 2. OFFRES ENCORE VALIDES ==========

    // Récupère les offres avec le statut demandé.
    // La date d'expiration doit être aujourd'hui ou dans le futur.
    List<Offre> findByStatutAndDateExpirationGreaterThanEqual(OffreStatut statut, LocalDate date);

}
