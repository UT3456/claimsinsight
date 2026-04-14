package com.claim360.fraudrisk.dto;

import com.claim360.fraudrisk.enums.IndicatorType;
import com.claim360.fraudrisk.validation.ValidateClaimId;
import com.claim360.fraudrisk.validation.ValidateSeverity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RiskIndicatorRequest {

    @NotBlank(message = "{com.claim360.fraudrisk.dto.RiskIndicatorRequest.claimId.blank}")
    @ValidateClaimId
    private String claimId;

    @NotNull(message = "{com.claim360.fraudrisk.dto.RiskIndicatorRequest.indicatorType.null}")
    private IndicatorType indicatorType;

    @NotBlank(message = "{com.claim360.fraudrisk.dto.RiskIndicatorRequest.severity.blank}")
    @ValidateSeverity
    private String severity;

    @NotNull(message = "{com.claim360.fraudrisk.dto.RiskIndicatorRequest.triggeredDate.null}")
    private LocalDate triggeredDate;
}