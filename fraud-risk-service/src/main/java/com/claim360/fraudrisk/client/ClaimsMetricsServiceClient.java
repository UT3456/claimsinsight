package com.claim360.fraudrisk.client;

import com.claim360.fraudrisk.client.dto.ClaimKpiDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * Feign client for claims-metrics-service — fetches KPI metrics for a claim
 * so the fraud-risk HighCost rule can use the portfolio-relative SEVERITY score
 * rather than raw claim-amount thresholds.
 *
 * <p>Inject with {@code @Autowired(required = false)}; the fraud-risk evaluation
 * falls back to raw amount thresholds when claims-metrics-service is unavailable.</p>
 */
@FeignClient(name = "claims-metrics-service", path = "/api/kpis")
public interface ClaimsMetricsServiceClient {

    /**
     * Returns all KPI records of a given metric type for a specific claim.
     *
     * @param claimId    the claim identifier
     * @param metricName the metric name (e.g. {@code "SEVERITY"})
     */
    @GetMapping("/claim/{claimId}/metric/{metricName}")
    List<ClaimKpiDTO> getKpisByClaimIdAndMetricName(
            @PathVariable("claimId") String claimId,
            @PathVariable("metricName") String metricName);
}
