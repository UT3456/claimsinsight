package com.claims.client.dto;

import lombok.Data;

import java.time.LocalDate;

/** Minimal projection of fraud-risk-service RiskScoreResponse. */
@Data
public class FraudRiskScoreDTO {
    private Long scoreId;
    private String claimId;
    private Double scoreValue;
    private LocalDate computedDate;
}
