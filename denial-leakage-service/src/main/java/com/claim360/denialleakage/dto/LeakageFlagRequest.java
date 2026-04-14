package com.claim360.denialleakage.dto;

import com.claim360.denialleakage.enums.LeakageType;
import com.claim360.denialleakage.validation.ValidateClaimId;
import com.claim360.denialleakage.validation.ValidateEstimatedLoss;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.Data;

import java.time.LocalDate;

@Data
public class LeakageFlagRequest {


    @NotBlank(message = "{com.claim360.denialleakage.dto.LeakageFlagRequest.claimId.blank}")
    @ValidateClaimId
    private String claimId;

    @NotNull(message = "{com.claim360.denialleakage.dto.LeakageFlagRequest.leakageType.null}")
    private LeakageType leakageType;

    @NotNull(message = "{com.claim360.denialleakage.dto.LeakageFlagRequest.estimatedLoss.null}")
    @ValidateEstimatedLoss
    private Double estimatedLoss;


    @NotNull(message = "{com.claim360.denialleakage.dto.LeakageFlagRequest.identifiedDate.null}")
    @PastOrPresent(message="Date must not exceed the current date")
    private LocalDate identifiedDate;
}