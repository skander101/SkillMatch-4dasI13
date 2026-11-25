package tn.esprit.skillmatch.alertes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.skillmatch.alertes.entity.OffreSauvegardee;

import java.util.List;

public interface OffreSauvegardeeRepository extends JpaRepository<OffreSauvegardee, Long> {

    List<OffreSauvegardee> findByUserIdOrderByDateDesc(Long userId);

    List<OffreSauvegardee> findAllByOrderByDateDesc();

    boolean existsByUserIdAndOfferId(Long userId, Long offerId);
}
