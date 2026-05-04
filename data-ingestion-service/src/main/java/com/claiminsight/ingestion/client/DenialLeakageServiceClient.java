package com.claiminsight.ingestion.client;

import com.claiminsight.ingestion.client.dto.AutoAnalyzeDenialRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign client for denial-leakage-service — triggers automatic denial pattern
 * analysis and leakage detection for a claim immediately after ingestion.
 *
 * <p>The call is fire-and-forget (non-blocking): a failure here must never roll
 * back or reject an otherwise valid ingestion. Inject with
 * {@code @Autowired(required = false)} so startup does not fail when
 * denial-leakage-service is temporarily down.</p>
 *
 * <p>Checks: DenialPattern creation when payload contains a denial code or
 * status=DENIED; Overpayment LeakageFlag when claimAmount exceeds policyLimit.</p>
 */
@FeignClient(name = "denial-leakage-service", path = "/api/denial-patterns")
public interface DenialLeakageServiceClient {

    /**
     * Instructs denial-leakage-service to analyze the payload for denial codes
     * and leakage indicators, then persist matching records.
     *
     * @param request contains claimId and the raw payloadJson string
     */
    @PostMapping("/auto-analyze")
    ResponseEntity<?> autoAnalyzeDenial(@RequestBody AutoAnalyzeDenialRequest request);
}
