package com.claims.dto.response;

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
public class ClaimReserveResponse {

    private Long reserveId;
    private String claimId;
    private BigDecimal reserveAmount;
    private LocalDate updatedDate;
}
