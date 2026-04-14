package com.claim360.denialleakage.repository;

import com.claim360.denialleakage.entity.DenialPattern;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DenialPatternRepository extends JpaRepository<DenialPattern, Long> {

    // Find all denial patterns
    List<DenialPattern> findByClaimId(String claimId);

    // Find  by denial code
    List<DenialPattern> findByDenialCode(String denialCode);

    // Findby reason containing keyword
    List<DenialPattern> findByReasonContainingIgnoreCase(String keyword);

    // Find by claim and denial code
    List<DenialPattern> findByClaimIdAndDenialCode(String claimId, String denialCode);
}