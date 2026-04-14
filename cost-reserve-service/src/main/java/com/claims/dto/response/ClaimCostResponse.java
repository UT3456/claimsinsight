package com.claims.dto.response;

import com.claims.enums.CostType;
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
public class ClaimCostResponse {

    private Long costId;
    private String claimId;
    private CostType costType;
    private BigDecimal amount;
    private LocalDate costDate;
}
