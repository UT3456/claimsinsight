package com.claims.dto.request;

import com.claims.enums.AgingBucket;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AgingRecordRequest {
    @NotBlank(message = "Claim ID is required")
    private String claimId;

    @NotNull(message = "Aging days is required")
    @Min(value = 0, message = "Aging days cannot be negative")
    private Integer agingDays;

    @NotNull(message = "Aging bucket is required")
    private AgingBucket agingBucket;
}
