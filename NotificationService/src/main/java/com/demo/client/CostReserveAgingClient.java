package com.demo.client;

import com.demo.client.dto.AgingRecordDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * Feign client for cost-reserve-service (aging endpoints).
 * Resolves via Eureka using spring.application.name = "cost-reserve-service".
 */
@FeignClient(name = "cost-reserve-service", path = "/api/aging")
public interface CostReserveAgingClient {

    /**
     * Returns all aging records in the given bucket (e.g. "BUCKET_90_PLUS").
     * Maps to: GET /api/aging/bucket/{agingBucket}
     */
    @GetMapping("/bucket/{agingBucket}")
    List<AgingRecordDTO> getAgingRecordsByBucket(@PathVariable("agingBucket") String agingBucket);
}
