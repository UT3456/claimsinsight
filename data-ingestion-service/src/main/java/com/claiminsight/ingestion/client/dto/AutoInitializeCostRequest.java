package com.claiminsight.ingestion.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload sent to cost-reserve-service's auto-initialize endpoint.
 * Carries the string claim ID and the raw payload JSON so the cost engine
 * can extract claimAmount, dates, and policy limits to seed ClaimCost,
 * ClaimReserve, and AgingRecord in a single call.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AutoInitializeCostRequest {
    private String claimId;
    private String payloadJson;
}
