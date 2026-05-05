package com.claim360.fraudrisk.dto;

import com.claim360.fraudrisk.entity.Investigation;
import com.claim360.fraudrisk.enums.InvestigationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvestigationDTO {
    private Long investigationId;
    private String claimId;
    private Long riskScoreId;
    private InvestigationStatus status;
    private String assignedTo;
    private String openedBy;
    private LocalDateTime openedAt;
    private LocalDateTime closedAt;
    private String notes;

    public static InvestigationDTO from(Investigation i) {
        return InvestigationDTO.builder()
                .investigationId(i.getInvestigationId())
                .claimId(i.getClaimId())
                .riskScoreId(i.getRiskScoreId())
                .status(i.getStatus())
                .assignedTo(i.getAssignedTo())
                .openedBy(i.getOpenedBy())
                .openedAt(i.getOpenedAt())
                .closedAt(i.getClosedAt())
                .notes(i.getNotes())
                .build();
    }
}
