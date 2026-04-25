package com.claiminsight.metrics.dto;

import com.claiminsight.metrics.model.ClaimStatus.ClaimStatusValue;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Request body for updating a claim's ACTIVE/INACTIVE status. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ClaimStatusRequestDTO {

    @NotNull
    private ClaimStatusValue status;
}
