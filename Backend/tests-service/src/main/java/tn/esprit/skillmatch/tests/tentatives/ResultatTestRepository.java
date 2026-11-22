package tn.esprit.skillmatch.tests.tentatives;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ResultatTestRepository extends JpaRepository<ResultatTest, Long> {
    int countByTest_IdAndCandidatId(Long testId, Long candidatId);

    Optional<ResultatTest> findFirstByTest_IdAndCandidatIdAndReussiTrueOrderByTentativeAsc(Long testId, Long candidatId);
}
