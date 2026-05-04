package com.claim360.fraudrisk.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for the auto-evaluate endpoint called by data-ingestion-service
 * after a claim is ingested. No @ValidateClaimId — claim IDs from ingestion
 * can be any string format (e.g. "CLM-2026-AUTO-001").
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AutoEvaluateRequest {
    /** String claim identifier from data-ingestion-service. */
    private String claimId;
    /** Raw JSON payload from the ingested ClaimRaw record. */
    private String payloadJson;
}
