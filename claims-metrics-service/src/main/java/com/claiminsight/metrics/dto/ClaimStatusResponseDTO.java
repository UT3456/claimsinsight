package com.claiminsight.metrics.dto;

import com.claiminsight.metrics.model.ClaimStatus.ClaimStatusValue;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ClaimStatusResponseDTO {
    private String claimId;
    private ClaimStatusValue status;
    private Instant updatedAt;
}
