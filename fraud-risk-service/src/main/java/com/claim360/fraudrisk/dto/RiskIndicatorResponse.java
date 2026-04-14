package com.claim360.fraudrisk.dto;

import com.claim360.fraudrisk.enums.IndicatorType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RiskIndicatorResponse {

    private Long indicatorId;
    private String claimId;
    private IndicatorType indicatorType;
    private String severity;
    private LocalDate triggeredDate;
}