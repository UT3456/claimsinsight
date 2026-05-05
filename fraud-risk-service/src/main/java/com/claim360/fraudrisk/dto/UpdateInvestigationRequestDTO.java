package com.claim360.fraudrisk.dto;

import com.claim360.fraudrisk.enums.InvestigationStatus;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Payload for PATCH /api/investigations/{id}.
 * All fields optional — only provided fields are updated.
 */
@Data
public class UpdateInvestigationRequestDTO {
    private InvestigationStatus status;
    @Size(max = 100)
    private String assignedTo;
    @Size(max = 2000)
    private String notes;
}
