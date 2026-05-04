package com.claiminsight.ingestion.client;

import com.claiminsight.ingestion.client.dto.AutoInitializeCostRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign client for cost-reserve-service — triggers automatic cost, reserve, and
 * aging record initialisation for a claim immediately after it is ingested.
 *
 * <p>The call is fire-and-forget (non-blocking): a failure here must never roll
 * back or reject an otherwise valid ingestion. Inject with
 * {@code @Autowired(required = false)} so startup does not fail when
 * cost-reserve-service is temporarily down.</p>
 *
 * <p>Creates: ClaimCost (SETTLEMENT type, seeded from claimAmount),
 * ClaimReserve (120% of claimAmount as initial estimate),
 * AgingRecord (bucket derived from incidentDate elapsed days).</p>
 */
@FeignClient(name = "cost-reserve-service", path = "/api/costs")
public interface CostReserveServiceClient {

    /**
     * Instructs cost-reserve-service to seed cost, reserve, and aging records
     * for this claim using data extracted from the raw payload JSON.
     *
     * @param request contains claimId and the raw payloadJson string
     */
    @PostMapping("/auto-initialize")
    ResponseEntity<?> autoInitializeCost(@RequestBody AutoInitializeCostRequest request);
}
