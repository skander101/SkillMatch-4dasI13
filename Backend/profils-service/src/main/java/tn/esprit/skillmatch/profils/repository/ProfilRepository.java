package tn.esprit.skillmatch.profils.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.skillmatch.profils.entity.CV;
import tn.esprit.skillmatch.profils.entity.Profil;

import java.util.List;
import java.util.Optional;

public interface ProfilRepository extends JpaRepository<Profil, Long> {

    Optional<Profil> findByUserId(Long userId);

    boolean existsByUserId(Long userId);
}
