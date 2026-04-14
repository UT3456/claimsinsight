package com.claim360.fraudrisk.repository;

import com.claim360.fraudrisk.entity.RiskIndicator;
import com.claim360.fraudrisk.enums.IndicatorType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RiskIndicatorRepository extends JpaRepository<RiskIndicator, Long> {

    // Find all indicators for a specific claim
    List<RiskIndicator> findByClaimId(String claimId);

    // Find all indicators by type
    List<RiskIndicator> findByIndicatorType(IndicatorType indicatorType);

    // Find all indicators by severity
    List<RiskIndicator> findBySeverity(String severity);

    // Find all indicators for a claim filtered by type
    List<RiskIndicator> findByClaimIdAndIndicatorType(String claimId, IndicatorType indicatorType);
}