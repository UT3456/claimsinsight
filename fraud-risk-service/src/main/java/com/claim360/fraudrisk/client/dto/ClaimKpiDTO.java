package com.claim360.fraudrisk.client.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Lightweight projection of a ClaimKPI record returned by claims-metrics-service.
 * Only the fields needed by the fraud-risk rule engine are mapped here.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClaimKpiDTO {
    private Long kpiId;
    private String claimId;
    private String metricName;
    private double metricValue;
    private LocalDate metricDate;
}
