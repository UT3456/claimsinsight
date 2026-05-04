package com.claiminsight.ingestion.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload sent to fraud-risk-service's auto-evaluate endpoint.
 * Carries the string claim ID and the raw payload JSON so the fraud engine
 * can extract claimAmount, dates, and policy fields without a separate DB call.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AutoEvaluateRiskRequest {
    private String claimId;
    private String payloadJson;
}
