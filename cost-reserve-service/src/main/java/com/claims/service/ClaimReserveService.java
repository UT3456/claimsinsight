
package com.claims.service;

import com.claims.dto.request.ClaimReserveRequest;
import com.claims.dto.response.ClaimReserveResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface ClaimReserveService {

    ClaimReserveResponse createClaimReserve(ClaimReserveRequest request);
    ClaimReserveResponse getClaimReserveById(Long reserveId);
    List<ClaimReserveResponse> getAllClaimReserves();
    ClaimReserveResponse updateClaimReserve(Long reserveId, ClaimReserveRequest request);
    void deleteClaimReserve(Long reserveId);

    List<ClaimReserveResponse> getReservesByClaimId(String claimId);
    List<ClaimReserveResponse> getReservesByDateRange(LocalDate startDate, LocalDate endDate);

    ClaimReserveResponse getLatestReserveForClaim(String claimId);
    BigDecimal getTotalReserveAmount();
    List<ClaimReserveResponse> getReserveHistoryByClaimId(String claimId);
    Map<String, BigDecimal> getLatestReserveSummaryAllClaims();
    Map<String, BigDecimal> getMonthlyReserveTrendByClaimId(String claimId);
    Map<String, Object> getReserveAdequacyForClaim(String claimId, BigDecimal totalCost);
}