package com.claiminsight.ingestion.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload sent to denial-leakage-service's auto-analyze endpoint.
 * Carries the string claim ID and the raw payload JSON so the denial engine
 * can detect denial codes, statuses, and overpayment conditions.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AutoAnalyzeDenialRequest {
    private String claimId;
    private String payloadJson;
}
