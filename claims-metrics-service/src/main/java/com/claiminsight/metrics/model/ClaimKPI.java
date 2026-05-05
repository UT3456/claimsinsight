package com.claiminsight.metrics.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** JPA entity representing a KPI metric record stored in the claim_kpi table. */
@Entity
@Table(name = "claim_kpi", indexes = {
    @Index(name = "idx_kpi_claim_id",     columnList = "claim_id"),
    @Index(name = "idx_kpi_metric_name",  columnList = "metric_name"),
    @Index(name = "idx_kpi_claim_metric", columnList = "claim_id,metric_name")
})
@Getter @Setter @NoArgsConstructor
public class ClaimKPI {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "kpi_id")
    private Long kpiId;

    @Column(name = "claim_id", nullable = false, length = 100)
    private String claimId;

    @Enumerated(EnumType.STRING)
    @Column(name = "metric_name", nullable = false, length = 50)
    private MetricName metricName;

    @Column(name = "metric_value", nullable = false, precision = 15, scale = 4)
    private BigDecimal metricValue;

    @Column(name = "metric_date", nullable = false)
    private LocalDate metricDate;
}
