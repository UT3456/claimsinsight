package com.claims.client;

import com.claims.client.dto.ClaimReserveDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/** Feign client for cost-reserve-service — fetches reserve history for analytics reports. */
@FeignClient(name = "cost-reserve-service", path = "/api/reserves",
        contextId = "costReserveServiceClient")
public interface CostReserveServiceClient {

    @GetMapping("/claim/{claimId}/history")
    List<ClaimReserveDTO> getReserveHistoryByClaimId(@PathVariable("claimId") String claimId);
}
