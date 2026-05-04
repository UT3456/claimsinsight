package com.claims.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

/**
 * Feign client for cost-reserve-service aging analytics endpoints.
 * Resolves via Eureka using spring.application.name = "cost-reserve-service".
 *
 * <p>This is a second client for cost-reserve-service, complementing
 * {@link CostReserveServiceClient} (which covers reserve history). The aging
 * endpoints live at a different base path (/api/aging) and are kept separate
 * to avoid path conflicts in Feign's routing.</p>
 */
@FeignClient(name = "cost-reserve-service", path = "/api/aging",
        contextId = "costReserveAgingClient")
public interface CostReserveAgingClient {

    /**
     * Returns portfolio-level aging health.
     * Maps to: GET /api/aging/analytics/portfolio-health
     * Response: { totalClaims, averageAgingDays, criticalClaims_90Plus, bucketDistributionPercentage }
     */
    @GetMapping("/analytics/portfolio-health")
    Map<String, Object> getPortfolioAgingHealth();

    /**
     * Returns count of aging records per bucket.
     * Maps to: GET /api/aging/analytics/distribution
     * Response: { "BUCKET_0_30": 45, "BUCKET_31_60": 22, ... }
     */
    @GetMapping("/analytics/distribution")
    Map<String, Long> getAgingBucketDistribution();
}
