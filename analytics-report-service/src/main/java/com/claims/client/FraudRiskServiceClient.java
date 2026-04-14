package com.claims.client;

import com.claims.client.dto.FraudRiskScoreDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/** Feign client for fraud-risk-service — fetches risk scores for analytics reports. */
@FeignClient(name = "fraud-risk-service", path = "/api/risk-scores")
public interface FraudRiskServiceClient {

    @GetMapping("/claim/{claimId}")
    List<FraudRiskScoreDTO> getRiskScoresByClaimId(@PathVariable("claimId") String claimId);
}
