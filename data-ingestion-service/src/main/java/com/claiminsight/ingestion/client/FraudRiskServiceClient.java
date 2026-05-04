package com.claiminsight.ingestion.client;

import com.claiminsight.ingestion.client.dto.AutoEvaluateRiskRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign client for fraud-risk-service — triggers automatic fraud risk evaluation
 * for a claim immediately after it is successfully ingested.
 *
 * <p>The call is fire-and-forget (non-blocking): a failure here must never roll
 * back or reject an otherwise valid ingestion. Inject with
 * {@code @Autowired(required = false)} so startup does not fail when
 * fraud-risk-service is temporarily down.</p>
 *
 * <p>Evaluates: HighCost rule (claimAmount > $10k), UnusualTiming rule
 * (filed < 30 days after policy start), weighted score computation (capped at 100).
 * Fires a notification to FRAUD + MANAGER roles if score ≥ 75.</p>
 */
@FeignClient(name = "fraud-risk-service", path = "/api/risk-scores")
public interface FraudRiskServiceClient {

    /**
     * Instructs fraud-risk-service to evaluate all fraud rules for this claim
     * using the raw payload JSON, then persist RiskIndicators + RiskScore.
     *
     * @param request contains claimId and the raw payloadJson string
     */
    @PostMapping("/auto-evaluate")
    ResponseEntity<?> autoEvaluateRisk(@RequestBody AutoEvaluateRiskRequest request);
}
