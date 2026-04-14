package com.demo.repositories;

import com.demo.entities.AdjusterPerformance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for AdjusterPerformance entity.
 */
public interface AdjusterPerformanceRepository extends JpaRepository<AdjusterPerformance, Long> {

    Optional<AdjusterPerformance> findByAdjusterIdAndPeriod(Long adjusterId, String period);

    List<AdjusterPerformance> findByPeriod(String period);

    List<AdjusterPerformance> findByPeriodOrderByErrorRateAscSlaBreachedCountAsc(String period);

    List<AdjusterPerformance> findByClaimsHandledLessThanAndPeriod(int threshold, String period);

    List<AdjusterPerformance> findByErrorRateGreaterThanAndPeriod(double errorRateThreshold, String period);

    List<AdjusterPerformance> findByClaimsHandledGreaterThanAndPeriod(int threshold, String period);

    @Query("SELECT a FROM AdjusterPerformance a " +
            "WHERE a.period = :period " +
            "AND a.claimsHandled > 0 " +
            "AND (a.deniedClaimsCount * 100.0 / a.claimsHandled) > :benchmark")
    List<AdjusterPerformance> findByHighDenialRate(@Param("period") String period, @Param("benchmark") double benchmark);

    @Query("SELECT a FROM AdjusterPerformance a " +
            "WHERE a.period = :period " +
            "AND a.avgTat > :slaTatTarget")
    List<AdjusterPerformance> findSlowPerformers(@Param("period") String period, @Param("slaTatTarget") double slaTatTarget);

    @Query("SELECT a FROM AdjusterPerformance a " +
            "WHERE a.period = :period " +
            "AND (a.slaMetCount + a.slaBreachedCount) > 0 " +
            "AND (a.slaMetCount * 1.0 / (a.slaMetCount + a.slaBreachedCount)) < 0.95")
    List<AdjusterPerformance> findBelowSlaComplianceThreshold(@Param("period") String period);

    @Query("SELECT AVG(a.claimsHandled) FROM AdjusterPerformance a WHERE a.period = :period")
    Double findAverageClaimsHandledByPeriod(@Param("period") String period);
}