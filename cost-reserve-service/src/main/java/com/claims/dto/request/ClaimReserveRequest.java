package com.claims.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClaimReserveRequest {

    @NotBlank(message = "Claim ID is required")
    private String claimId;

    @NotNull(message = "Reserve amount is required")
    @DecimalMin(value = "0.01", message = "Reserve amount must be greater than 0")
    private BigDecimal reserveAmount;

    @NotNull(message = "Updated date is required")
    private LocalDate updatedDate;
}
