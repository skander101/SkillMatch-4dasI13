package tn.esprit.skillmatch.tests.catalogue;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TestTechniqueRepository extends JpaRepository<TestTechnique, Long> {

    List<TestTechnique> findAllByCompetenceIgnoreCaseOrderByIdAsc(String competence);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select test from TestTechnique test where test.id = :id")
    Optional<TestTechnique> findLockedById(@Param("id") Long id);
}
