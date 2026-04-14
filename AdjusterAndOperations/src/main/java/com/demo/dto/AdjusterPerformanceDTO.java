package com.demo.dto;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * Data Transfer Object representing the performance metrics of a claims adjuster.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class AdjusterPerformanceDTO {

    private Long perfId;

//    @Schema(defaultValue = "ID must be positive")
    @NotNull(message = "{com.demo.dto.AdjusterPerformanceDTO.adjusterId.error}")
    @Positive(message = "{com.demo.dto.AdjusterPerformanceDTO.adjusterId.error}")
    private Long adjusterId;

    /** * Total number of insurance claims processed during the period.
     */
    @Min(value = 0, message = "{com.demo.dto.AdjusterPerformanceDTO.claimsHandled.error}")
    private int claimsHandled;

    @Min(value = 0, message = "{com.demo.dto.AdjusterPerformanceDTO.totalDaysTaken.error}")
    private int totalDaysTaken;

    @Min(value = 0, message = "{com.demo.dto.AdjusterPerformanceDTO.slaMetCount.error}")
    private int slaMetCount;

    @Min(value = 0, message = "{com.demo.dto.AdjusterPerformanceDTO.slaBreachedCount.error}")
    private int slaBreachedCount;

    @Min(value = 0, message = "{com.demo.dto.AdjusterPerformanceDTO.deniedClaimsCount.error}")
    private int deniedClaimsCount;

    @DecimalMin(value = "0.0", message = "{com.demo.dto.AdjusterPerformanceDTO.errorRate.error}")
    @DecimalMax(value = "100.0", message = "{com.demo.dto.AdjusterPerformanceDTO.errorRate.error}")
    private double errorRate;

    @NotBlank(message = "{com.demo.dto.AdjusterPerformanceDTO.period.blank}")
    private String period;

    private double avgTat;

    private double qualityScore;

    private double denialRate;

    private double slaComplianceRate;

    private double performanceIndex;

    private String productivityFlag;

    private String performanceFlag;
}