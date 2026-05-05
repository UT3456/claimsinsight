package com.claims.entity;

import com.claims.enums.ReportScope;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "analytics_report", indexes = {
    @Index(name = "idx_ar_scope",          columnList = "scope"),
    @Index(name = "idx_ar_scope_value",    columnList = "scope_value"),
    @Index(name = "idx_ar_generated_date", columnList = "generated_date"),
    @Index(name = "idx_ar_generated_by",   columnList = "generated_by")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long reportId;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false)
    private ReportScope scope;

    // The specific value being scoped — e.g. "North Region", "Auto", "Q1-2024"
    @Column(name = "scope_value", nullable = false)
    private String scopeValue;

    // Comma-separated list of metrics this report covers
    // e.g. "TAT, LossRatio, ClaimVolume, DenialRate"
    @Column(name = "metrics", nullable = false, length = 1000)
    private String metrics;

    @Column(name = "generated_date", nullable = false)
    private LocalDate generatedDate;

    // Who triggered the report generation
    @Column(name = "generated_by")
    private String generatedBy;

    // Snapshot of the actual report data stored as a JSON string
    @Column(name = "report_data", columnDefinition = "TEXT")
    private String reportData;
}
