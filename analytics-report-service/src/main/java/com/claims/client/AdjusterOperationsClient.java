package com.claims.client;

import com.claims.client.dto.AdjusterPerformanceDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Feign client for AdjusterAndOperations service.
 * Resolves via Eureka using spring.application.name = "AdjusterAndOperations".
 */
@FeignClient(name = "AdjusterAndOperations", path = "/api/adjusters")
public interface AdjusterOperationsClient {

    /**
     * Returns all adjuster performance records, optionally scoped to a period.
     * Maps to: GET /api/adjusters/performance?period={period}
     */
    @GetMapping("/performance")
    List<AdjusterPerformanceDTO> getAllAdjusterPerformance(
            @RequestParam(value = "period", required = false) String period);

    /**
     * Returns the top-performing adjusters for the given period.
     * Maps to: GET /api/adjusters/top-performers?period={period}
     */
    @GetMapping("/top-performers")
    List<AdjusterPerformanceDTO> getTopPerformers(
            @RequestParam("period") String period);

    /**
     * Returns adjusters flagged for training in the given period.
     * Maps to: GET /api/adjusters/training-flagged?period={period}
     */
    @GetMapping("/training-flagged")
    List<AdjusterPerformanceDTO> getTrainingFlaggedAdjusters(
            @RequestParam("period") String period);
}
