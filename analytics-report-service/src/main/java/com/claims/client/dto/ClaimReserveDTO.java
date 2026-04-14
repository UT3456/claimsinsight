package com.claims.client.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Minimal projection of cost-reserve-service ClaimReserveResponse. */
@Data
public class ClaimReserveDTO {
    private Long reserveId;
    private String claimId;
    private BigDecimal reserveAmount;
    private LocalDate reserveDate;
}
