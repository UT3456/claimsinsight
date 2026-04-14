package com.claim360.fraudrisk.dto;

import com.claim360.fraudrisk.validation.ValidateClaimId;
import com.claim360.fraudrisk.validation.ValidateScoreValue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RiskScoreRequest {

    @NotBlank(message = "{com.claim360.fraudrisk.dto.RiskScoreRequest.claimId.blank}")
    @ValidateClaimId
    private String claimId;

    @NotNull(message = "{com.claim360.fraudrisk.dto.RiskScoreRequest.scoreValue.null}")
    @ValidateScoreValue
    private Double scoreValue;

    @NotNull(message = "{com.claim360.fraudrisk.dto.RiskScoreRequest.computedDate.null}")
    private LocalDate computedDate;
}