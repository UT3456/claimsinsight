package com.claim360.denialleakage.repository;

import com.claim360.denialleakage.entity.LeakageFlag;
import com.claim360.denialleakage.enums.LeakageType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeakageFlagRepository extends JpaRepository<LeakageFlag, Long> {

    // Find specific claim
    List<LeakageFlag> findByClaimId(String claimId);

    // Find leakage type
    List<LeakageFlag> findByLeakageType(LeakageType leakageType);

    // Find estimated loss greater than or equal to amount
    List<LeakageFlag> findByEstimatedLossGreaterThanEqual(Double amount);

    // Find by claim and leakage type
    List<LeakageFlag> findByClaimIdAndLeakageType(String claimId, LeakageType leakageType);

    /**
     * Aggregates leakage flags by type: returns rows of [LeakageType, count, totalEstimatedLoss].
     * Used by the leakage summary endpoint to give dashboard-ready rollup data.
     */
    @Query("SELECT l.leakageType, COUNT(l), SUM(l.estimatedLoss) FROM LeakageFlag l GROUP BY l.leakageType")
    List<Object[]> getLeakageSummaryGroupedByType();
}