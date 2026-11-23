package tn.esprit.skillmatch.profils.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.skillmatch.profils.entity.CV;

import java.util.List;

public interface CVRepository extends JpaRepository<CV, Long> {

    List<CV> findByProfilIdOrderByDateUploadDesc(Long profilId);
}
