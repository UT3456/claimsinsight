package com.claiminsight.ingestion.client;

import com.claiminsight.ingestion.client.dto.AutoGenerateSlaRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign client for AdjusterAndOperations — triggers automatic SLA violation
 * creation for a claim immediately after it is successfully ingested.
 *
 * The call is fire-and-forget (non-blocking): a failure here must never
 * reject an otherwise valid ingestion. Inject with
 * {@code @Autowired(required = false)} so startup does not fail when
 * AdjusterAndOperations is temporarily down (e.g. during rolling deploys).
 */
@FeignClient(name = "AdjusterAndOperations", path = "/api/sla-violations")
public interface AdjusterOperationsClient {

    /**
     * Instructs AdjusterAndOperations to create a default SLA tracking record
     * (CLAIM_ASSESSMENT, 30-day target, LOW severity) for the given claim ref.
     *
     * @param request contains the string claimRef and optional adjusterId
     */
    @PostMapping("/auto-generate")
    ResponseEntity<?> autoGenerateSlaViolation(@RequestBody AutoGenerateSlaRequest request);
}
