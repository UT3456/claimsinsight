package com.claims.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request payload for the auto-initialize endpoint called by data-ingestion-service
 * after a claim is ingested. Extracts claimAmount, dates, and policy limits from
 * the raw payloadJson to seed cost, reserve, and aging records.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AutoInitializeRequest {
    private String claimId;
    private String payloadJson;
}
