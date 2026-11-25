
package tn.esprit.skillmatch.offres.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.skillmatch.offres.entity.Candidature;
import java.util.List;


// ========== REPOSITORY DES CANDIDATURES ==========

// Interface qui permet d'accéder aux candidatures dans MySQL.
// JpaRepository fournit automatiquement les opérations CRUD.
public interface CandidatureRepository extends JpaRepository<Candidature, Long> {


    // ========== 1. VÉRIFIER LES CANDIDATURES D'UNE OFFRE ==========

    // Vérifie si une offre possède au moins une candidature.
    // Retourne true si oui, false sinon.
    boolean existsByOffreId(Long offreId);


    // ========== 2. VÉRIFIER UNE CANDIDATURE EXISTANTE ==========

    // Vérifie si un candidat a déjà postulé à une offre.
    // Permet d'éviter les candidatures en double.
    boolean existsByUserIdAndOffreId(Long userId, Long offreId);


    // ========== 3. CANDIDATURES D'UN CANDIDAT ==========

    // Récupère toutes les candidatures d'un candidat.
    List<Candidature> findByUserId(Long userId);


    // ========== 4. CANDIDATURES REÇUES PAR UN RECRUTEUR ==========

    // Récupère les candidatures des offres d'un recruteur.
    List<Candidature> findByOffreRecruteurId(Long recruteurId);

}
