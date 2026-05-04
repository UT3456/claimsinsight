package com.claim360.denialleakage.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for the auto-analyze endpoint called by data-ingestion-service
 * after a claim is ingested. No @ValidateClaimId constraint — claim IDs from
 * ingestion can be any string format (e.g. "CLM-2026-AUTO-001").
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AutoAnalyzeRequest {
    private String claimId;
    private String payloadJson;
}
