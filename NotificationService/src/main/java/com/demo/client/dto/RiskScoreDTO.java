package com.demo.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;

/**
 * Client-side DTO mirroring RiskScoreResponse from fraud-risk-service.
 * Fields: scoreId, claimId, scoreValue, computedDate.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RiskScoreDTO {

    private Long scoreId;
    private String claimId;
    private Double scoreValue;
    private LocalDate computedDate;
}
