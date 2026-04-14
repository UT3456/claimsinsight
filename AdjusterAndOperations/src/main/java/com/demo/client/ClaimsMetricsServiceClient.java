package com.demo.client;

import com.demo.client.dto.ClaimKpiDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * Feign client for claims-metrics-service.
 * Fetches KPI metrics for claims handled by adjusters.
 */
@FeignClient(name = "claims-metrics-service", path = "/api/kpis")
public interface ClaimsMetricsServiceClient {

    @GetMapping("/claim/{claimId}")
    List<ClaimKpiDTO> getKpisByClaimId(@PathVariable("claimId") String claimId);
}
