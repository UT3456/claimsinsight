package com.claiminsight.ingestion.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Feign client for claims-metrics-service — triggers automatic KPI calculation
 * for a claim immediately after it is successfully ingested.
 *
 * The call is fire-and-forget (non-blocking): a failure here must never roll
 * back or reject an otherwise valid ingestion. Inject with
 * {@code @Autowired(required = false)} so startup does not fail when
 * claims-metrics-service is temporarily down (e.g. during rolling deploys).
 */
@FeignClient(name = "claims-metrics-service", path = "/api/kpis")
public interface ClaimsMetricsServiceClient {

    /**
     * Instructs claims-metrics-service to compute TAT, CYCLE_TIME, SEVERITY,
     * FREQUENCY, LOSS_RATIO and SETTLEMENT_TIME for the given claim and persist
     * all 6 KPI records in one shot.
     *
     * @param claimId the business claim identifier (e.g. "CLM-2026-AUTO-001")
     */
    @PostMapping("/calculate/{claimId}")
    ResponseEntity<?> calculateKpis(@PathVariable("claimId") String claimId);
}
