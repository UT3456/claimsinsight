package com.claims.client;

import com.claims.client.dto.LeakageFlagDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Map;

/** Feign client for denial-leakage-service — fetches leakage flags for analytics reports. */
@FeignClient(name = "denial-leakage-service", path = "/api/leakage-flags")
public interface DenialLeakageServiceClient {

    /**
     * Pre-aggregated leakage summary grouped by type.
     * Maps to: GET /api/leakage-flags/summary
     * Response: { totalFlags, totalEstimatedLoss, breakdown: [...] }
     */
    @GetMapping("/summary")
    Map<String, Object> getLeakageSummaryByType();

    /** Leakage flags for a single claim. */
    @GetMapping("/claim/{claimId}")
    List<LeakageFlagDTO> getLeakageFlagsByClaimId(@PathVariable("claimId") String claimId);
}
