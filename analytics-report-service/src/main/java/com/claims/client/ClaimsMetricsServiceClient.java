package com.claims.client;

import com.claims.client.dto.ClaimKpiDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/** Feign client for claims-metrics-service — fetches KPI metrics for analytics reports. */
@FeignClient(name = "claims-metrics-service", path = "/api/kpis")
public interface ClaimsMetricsServiceClient {

    @GetMapping("/claim/{claimId}")
    List<ClaimKpiDTO> getKpisByClaimId(@PathVariable("claimId") String claimId);
}
