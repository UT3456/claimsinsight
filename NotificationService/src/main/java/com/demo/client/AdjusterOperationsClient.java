package com.demo.client;

import com.demo.client.dto.SLAViolationDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * Feign client for AdjusterAndOperations service.
 * Resolves via Eureka using spring.application.name = "AdjusterAndOperations".
 */
@FeignClient(name = "AdjusterAndOperations", path = "/api/sla-violations")
public interface AdjusterOperationsClient {

    /**
     * Returns all SLA violations across all adjusters.
     * Maps to: GET /api/sla-violations
     * The caller filters by date in memory for monthly scoping.
     */
    @GetMapping
    List<SLAViolationDTO> getAllViolations();
}
