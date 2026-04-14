package com.claim360.fraudrisk.repository;

import com.claim360.fraudrisk.entity.RiskScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RiskScoreRepository extends JpaRepository<RiskScore, Long> {

    // Find all scores for a specific claim
    List<RiskScore> findByClaimId(String claimId);

    // Find the latest score for a specific claim
    Optional<RiskScore> findTopByClaimIdOrderByComputedDateDesc(String claimId);

    // Find all scores above a threshold
    List<RiskScore> findByScoreValueGreaterThanEqual(Double threshold);
}