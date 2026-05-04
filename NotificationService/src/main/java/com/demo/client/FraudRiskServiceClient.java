package com.demo.client;

import com.demo.client.dto.RiskScoreDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * Feign client for fraud-risk-service.
 * Resolves via Eureka using spring.application.name = "fraud-risk-service".
 */
@FeignClient(name = "fraud-risk-service", path = "/api/risk-scores")
public interface FraudRiskServiceClient {

    /**
     * Returns all risk scores whose scoreValue is at or above {@code threshold}.
     * Maps to: GET /api/risk-scores/threshold/{threshold}
     */
    @GetMapping("/threshold/{threshold}")
    List<RiskScoreDTO> getRiskScoresAboveThreshold(@PathVariable("threshold") Double threshold);
}
