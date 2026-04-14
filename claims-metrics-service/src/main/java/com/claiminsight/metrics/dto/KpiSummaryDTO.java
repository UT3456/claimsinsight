package com.claiminsight.metrics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

/** Response DTO containing all computed KPI metrics for a single claim. */
@Data
@AllArgsConstructor
public class KpiSummaryDTO {

    private String claimId;

    /** Turnaround Time — days from incident date to today. */
    private BigDecimal tat;

    /** Cycle Time — days between first and most recent ingestion of this claim. */
    private BigDecimal cycleTime;

    /** Severity score (1–10) derived from claim amount. */
    private BigDecimal severity;

    /** Frequency — number of raw records ingested for this claim. */
    private BigDecimal frequency;

    /** Loss Ratio — claimAmount / premium amount from payload (if available). */
    private BigDecimal lossRatio;
    /** Settlement Time – days from incident/filing date to settlement/closure date. */
    private BigDecimal settlementTime;
    /** Date the KPIs were calculated. */
    private LocalDate calculatedDate;

    /** Individual KPI records saved to the database. */
    private List<ClaimKpiResponseDTO> savedKpis;
}
