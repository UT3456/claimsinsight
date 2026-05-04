package com.claims.client;

import com.claims.client.dto.ClaimKpiDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/** Feign client for claims-metrics-service — fetches KPI metrics for analytics reports. */
@FeignClient(name = "claims-metrics-service", path = "/api/kpis")
public interface ClaimsMetricsServiceClient {

    /** All KPI records across all claims — used for portfolio-level dashboards. */
    @GetMapping
    List<ClaimKpiDTO> getAllKpis();

    /** All KPI records for a specific metric type (e.g. "TAT", "SEVERITY"). */
    @GetMapping("/metric/{metricName}")
    List<ClaimKpiDTO> getKpisByMetricName(@PathVariable("metricName") String metricName);

    /** KPI records for a single claim. */
    @GetMapping("/claim/{claimId}")
    List<ClaimKpiDTO> getKpisByClaimId(@PathVariable("claimId") String claimId);
}
