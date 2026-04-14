package com.demo.dto;

import com.demo.validation.ValidateViolationType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.Date;

/**
 * Data Transfer Object for tracking Service Level Agreement (SLA) Violations.
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class SLAViolationDTO {

    private Long violationId;

    @NotNull(message = "{com.demo.dto.SLAViolationDTO.claimId.blank}")
    @Positive(message = "Claim ID must be positive")
    private Long claimId;

    @NotNull(message = "{com.demo.dto.SLAViolationDTO.adjusterId.error}")
    @Positive(message = "{com.demo.dto.SLAViolationDTO.adjusterId.error}")
    private Long adjusterId;

    @NotBlank(message = "{com.demo.dto.SLAViolationDTO.violationType.blank}")
    @ValidateViolationType(message = "{com.demo.dto.SLAViolationDTO.violationType.error}")
    private String violationType;

    @Min(value = 1, message = "{com.demo.dto.SLAViolationDTO.slaTargetDays.error}")
    private int slaTargetDays;

    @Min(value = 1, message = "{com.demo.dto.SLAViolationDTO.actualDays.error}")
    private int actualDays;

    @NotNull(message = "{com.demo.dto.SLAViolationDTO.violationDate.blank}")
    private Date violationDate;

    /**
     * The number of days the task exceeded the SLA target.
     * <p>Formula: {@code actualDays - slaTargetDays}.</p>
     */
    private int daysOverdue;

    private String severity;

    private boolean escalated;
}