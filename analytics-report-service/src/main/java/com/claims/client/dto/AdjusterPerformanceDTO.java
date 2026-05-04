package com.claims.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Client-side projection of AdjusterAndOperations AdjusterPerformanceDTO.
 * Only fields used by analytics dashboards and report generation are mapped;
 * unknown fields are silently ignored.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AdjusterPerformanceDTO {

    private Long perfId;
    private Long adjusterId;
    private Integer claimsHandled;

    /** Average turn-around time in days. */
    private Double avgTat;

    /** Quality score (0–100). */
    private Double qualityScore;

    /** Denial rate as a percentage (0–100). */
    private Double denialRate;

    /** SLA compliance rate as a percentage (0–100). */
    private Double slaComplianceRate;

    /** Composite performance index. */
    private Double performanceIndex;

    private Integer slaMetCount;
    private Integer slaBreachedCount;
    private Integer deniedClaimsCount;

    /** e.g. "HIGH_PRODUCTIVITY", "LOW_PRODUCTIVITY". */
    private String productivityFlag;

    /** e.g. "SATISFACTORY", "TRAINING_REQUIRED". */
    private String performanceFlag;

    private Double errorRate;

    /** Period label, e.g. "2025-05". */
    private String period;
}
