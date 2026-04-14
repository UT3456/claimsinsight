package com.claiminsight.metrics.client;

import com.claiminsight.metrics.client.dto.ClaimRawDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * Feign client for data-ingestion-service.
 * Uses Eureka service name for load-balanced discovery — no hardcoded URLs.
 */
@FeignClient(name = "data-ingestion-service")
public interface DataIngestionClient {

    /**
     * Fetches all raw claim records for the given claimId.
     * Calls GET /api/ingest/raw-claims/{claimId} on data-ingestion-service.
     */
    @GetMapping("/api/ingest/raw-claims/{claimId}")
    List<ClaimRawDTO> getRawClaimsByClaimId(@PathVariable("claimId") String claimId);
}
