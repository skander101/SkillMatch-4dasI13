package tn.esprit.skillmatch.entreprises.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.skillmatch.entreprises.entity.Entreprise;

public interface EntrepriseRepository extends JpaRepository<Entreprise, Long> {
}
