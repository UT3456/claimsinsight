package com.claims.service;

import com.claims.dto.request.AgingRecordRequest;
import com.claims.dto.response.AgingRecordResponse;
import com.claims.enums.AgingBucket;

import java.util.List;
import java.util.Map;

public interface AgingRecordService {


    AgingRecordResponse createAgingRecord(AgingRecordRequest request);
    AgingRecordResponse getAgingRecordById(Long agingId);
    List<AgingRecordResponse> getAllAgingRecords();
    AgingRecordResponse updateAgingRecord(Long agingId, AgingRecordRequest request);
    void deleteAgingRecord(Long agingId);


    List<AgingRecordResponse> getAgingRecordsByClaimId(String claimId);
    List<AgingRecordResponse> getAgingRecordsByBucket(AgingBucket agingBucket);
    List<AgingRecordResponse> getClaimsAgedBeyond(Integer days);


    Map<String, Long> getAgingBucketDistribution();
    Map<String, Object> getAgingSummaryForClaim(String claimId);
    Map<String, Object> getPortfolioAgingHealth();
}
