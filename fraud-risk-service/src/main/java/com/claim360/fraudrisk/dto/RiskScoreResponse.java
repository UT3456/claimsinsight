package com.claim360.fraudrisk.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RiskScoreResponse {

    private Long scoreId;
    private String claimId;
    private Double scoreValue;
    private LocalDate computedDate;
}