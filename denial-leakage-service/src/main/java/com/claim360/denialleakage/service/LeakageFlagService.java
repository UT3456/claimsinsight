package com.claim360.denialleakage.service;

import com.claim360.denialleakage.dto.LeakageFlagRequest;
import com.claim360.denialleakage.dto.LeakageFlagResponse;
import com.claim360.denialleakage.enums.LeakageType;

import java.util.List;
import java.util.Map;

public interface LeakageFlagService {

    // Create
    LeakageFlagResponse createLeakageFlag(LeakageFlagRequest request);

    //by ID
    LeakageFlagResponse getLeakageFlagById(Long leakageId);

    // Get al
    List<LeakageFlagResponse> getAllLeakageFlags();

    //specificclaim
    List<LeakageFlagResponse> getLeakageFlagsByClaimId(String claimId);

    //leakage type
    List<LeakageFlagResponse> getLeakageFlagsByLeakageType(LeakageType leakageType);

    //estimated loss above amount
    List<LeakageFlagResponse> getLeakageFlagsByEstimatedLoss(Double amount);

    //claim and leakage type
    List<LeakageFlagResponse> getLeakageFlagsByClaimIdAndLeakageType(String claimId, LeakageType leakageType);

    // Update
    LeakageFlagResponse updateLeakageFlag(Long leakageId, LeakageFlagRequest request);

    // Delete
    void deleteLeakageFlag(Long leakageId);

    /**
     * Returns a dashboard-ready rollup: total flags, total estimated loss, and a
     * per-type breakdown grouped by {@link com.claim360.denialleakage.enums.LeakageType}.
     */
    Map<String, Object> getLeakageSummaryByType();
}