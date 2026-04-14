package com.claiminsight.metrics.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Response DTO returned after creating or fetching a KPI record. */
@Data
public class ClaimKpiResponseDTO {
    
    private Long kpiId;
    
    private String claimId;
    
    private String metricName;
    
    private BigDecimal metricValue;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate metricDate;
}
