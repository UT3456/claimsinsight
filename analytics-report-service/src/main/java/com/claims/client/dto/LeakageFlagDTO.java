package com.claims.client.dto;

import lombok.Data;

import java.time.LocalDate;

/** Minimal projection of denial-leakage-service LeakageFlagResponse. */
@Data
public class LeakageFlagDTO {
    private Long leakageId;
    private String claimId;
    private String leakageType;
    private Double estimatedLoss;
    private LocalDate flaggedDate;
}
