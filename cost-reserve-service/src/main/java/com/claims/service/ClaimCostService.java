




package com.claims.service;

import com.claims.dto.request.ClaimCostRequest;
import com.claims.dto.response.ClaimCostResponse;
import com.claims.enums.CostType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface ClaimCostService {

    ClaimCostResponse createClaimCost(ClaimCostRequest request);
    ClaimCostResponse getClaimCostById(Long costId);
    List<ClaimCostResponse> getAllClaimCosts();
    ClaimCostResponse updateClaimCost(Long costId, ClaimCostRequest request);
    void deleteClaimCost(Long costId);

    List<ClaimCostResponse> getCostsByClaimId(String claimId);
    List<ClaimCostResponse> getCostsByCostType(CostType costType);
    List<ClaimCostResponse> getCostsByClaimIdAndType(String claimId, CostType costType);
    List<ClaimCostResponse> getCostsByDateRange(LocalDate startDate, LocalDate endDate);

    BigDecimal getTotalCostByClaimId(String claimId);
    Map<String, BigDecimal> getCostBreakdownByTypeForClaim(String claimId);
    Map<String, BigDecimal> getOverallCostSummaryByType();
    Map<String, BigDecimal> getMonthlyCostTrendByClaimId(String claimId);
    Map<String, Object> getHighestCostClaim();

    java.util.Map<String, Object> autoInitializeClaim(String claimId, String payloadJson);
}

