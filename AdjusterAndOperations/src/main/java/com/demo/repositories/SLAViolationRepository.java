package com.demo.repositories;

import com.demo.entities.SLAViolation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;

/**
 * Repository interface for SLAViolation entity.
 */
public interface SLAViolationRepository extends JpaRepository<SLAViolation, Long> {

    List<SLAViolation> findByAdjusterId(Long adjusterId);

    List<SLAViolation> findByAdjusterIdAndViolationDateBetween(Long adjusterId, Date start, Date end);

    List<SLAViolation> findByClaimId(Long claimId);

    List<SLAViolation> findByViolationType(String violationType);

    List<SLAViolation> findByViolationDateBetween(Date start, Date end);

    @Query("SELECT s FROM SLAViolation s " +
            "WHERE (s.actualDays - s.slaTargetDays) > :minDaysOverdue " +
            "ORDER BY (s.actualDays - s.slaTargetDays) DESC")
    List<SLAViolation> findByDaysOverdueGreaterThan(@Param("minDaysOverdue") int minDaysOverdue);

    @Query("SELECT s FROM SLAViolation s " +
            "WHERE (s.actualDays - s.slaTargetDays) BETWEEN :minDays AND :maxDays")
    List<SLAViolation> findBySeverityRange(@Param("minDays") int minDays, @Param("maxDays") int maxDays);

    @Query("SELECT s FROM SLAViolation s " +
            "WHERE s.adjusterId = :adjusterId " +
            "AND (s.actualDays - s.slaTargetDays) > 5")
    List<SLAViolation> findEscalationCandidatesByAdjuster(@Param("adjusterId") Long adjusterId);

    @Query("SELECT COUNT(s) FROM SLAViolation s " +
            "WHERE s.adjusterId = :adjusterId " +
            "AND s.violationDate BETWEEN :start AND :end")
    Long countViolationsByAdjusterAndPeriod(@Param("adjusterId") Long adjusterId, @Param("start") Date start, @Param("end") Date end);

    @Query("SELECT COALESCE(SUM(s.actualDays - s.slaTargetDays), 0) " +
            "FROM SLAViolation s WHERE s.claimId = :claimId")
    Integer findTotalDaysOverdueByClaim(@Param("claimId") Long claimId);

    @Query("SELECT COUNT(s) FROM SLAViolation s WHERE s.adjusterId = :adjusterId")
    Long countAllViolationsByAdjuster(@Param("adjusterId") Long adjusterId);
}