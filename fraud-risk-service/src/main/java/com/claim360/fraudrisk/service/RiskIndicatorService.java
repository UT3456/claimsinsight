package com.claim360.fraudrisk.service;

import com.claim360.fraudrisk.dto.RiskIndicatorRequest;
import com.claim360.fraudrisk.dto.RiskIndicatorResponse;
import com.claim360.fraudrisk.enums.IndicatorType;

import java.util.List;

public interface RiskIndicatorService {

    // Create a new risk indicator
    RiskIndicatorResponse createRiskIndicator(RiskIndicatorRequest request);

    // Get a risk indicator by ID
    RiskIndicatorResponse getRiskIndicatorById(Long indicatorId);

    // Get all risk indicators
    List<RiskIndicatorResponse> getAllRiskIndicators();

    // Get all risk indicators for a specific claim
    List<RiskIndicatorResponse> getRiskIndicatorsByClaimId(String claimId);

    // Get all risk indicators by type
    List<RiskIndicatorResponse> getRiskIndicatorsByType(IndicatorType indicatorType);

    // Get all risk indicators by severity
    List<RiskIndicatorResponse> getRiskIndicatorsBySeverity(String severity);

    // Update a risk indicator
    RiskIndicatorResponse updateRiskIndicator(Long indicatorId, RiskIndicatorRequest request);

    // Delete a risk indicator
    void deleteRiskIndicator(Long indicatorId);
}