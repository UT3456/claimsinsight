package com.claim360.denialleakage.repository;

import com.claim360.denialleakage.entity.DenialPattern;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DenialPatternRepository extends JpaRepository<DenialPattern, Long> {

    List<DenialPattern> findByClaimId(String claimId);

    List<DenialPattern> findByDenialCode(String denialCode);

    List<DenialPattern> findByReasonContainingIgnoreCase(String keyword);

    List<DenialPattern> findByClaimIdAndDenialCode(String claimId, String denialCode);

    /** Used by the weekly denial-spike detection scheduled job. */
    long countByOccurrenceDateBetween(LocalDate startDate, LocalDate endDate);
}