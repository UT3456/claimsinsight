package com.claim360.fraudrisk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Payload for POST /api/investigations — fired when an analyst clicks "Escalate SIU". */
@Data
public class CreateInvestigationRequestDTO {

    @NotBlank(message = "claimId is required")
    @Size(max = 100)
    private String claimId;

    /** Optional — the RiskScore row that triggered the escalation. */
    private Long riskScoreId;

    /** Optional analyst note attached to the escalation. */
    @Size(max = 2000)
    private String notes;
}
