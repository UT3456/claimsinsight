package com.demo.client.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Minimal projection of claims-metrics-service ClaimKpiResponseDTO. */
@Data
public class ClaimKpiDTO {
    private Long kpiId;
    private String claimId;
    private String metricName;
    private BigDecimal metricValue;
    private LocalDate metricDate;
}
