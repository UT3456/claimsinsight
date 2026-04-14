package com.claim360.denialleakage.dto;

import com.claim360.denialleakage.validation.ValidateClaimId;
import com.claim360.denialleakage.validation.ValidateDenialCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.Data;

import java.time.LocalDate;

@Data
public class DenialPatternRequest {


    // Claim ID not be blank and follow CLM-[digits] pattern

    @NotBlank(message = "{com.claim360.denialleakage.dto.DenialPatternRequest.claimId.blank}")
    @ValidateClaimId
    private String claimId;


     // Denial Code must not be blank and follow pattern like CO-4, PR-96.

    @NotBlank(message = "{com.claim360.denialleakage.dto.DenialPatternRequest.denialCode.blank}")
    @ValidateDenialCode
    private String denialCode;


     // Reason must not be blank.

    @NotBlank(message = "{com.claim360.denialleakage.dto.DenialPatternRequest.reason.blank}")
    private String reason;


     // Occurrence date must not be null.

    @NotNull(message = "{com.claim360.denialleakage.dto.DenialPatternRequest.occurrenceDate.null}")
    @PastOrPresent(message="Date must not exceed the current date")
    private LocalDate occurrenceDate;
}