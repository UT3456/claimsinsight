package com.demo.entities;

import jakarta.persistence.*;
import lombok.*;

/**
 * Persistence entity representing the 'adjuster_performance' table in the database. 
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@Entity
@Table(name = "adjuster_performance", indexes = {
    @Index(name = "idx_ap_adjuster_period", columnList = "adjuster_id,period")
})
public class AdjusterPerformance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="perf_id")
    private long perfId;

    @Column(name="adjuster_id")
    private long adjusterId;

    @Column(name="claims_handled")
    private int claimsHandled;

    @Column(name = "total_days_taken", nullable = false)
    private int totalDaysTaken;

    @Column(name="avg_tat")
    private double avgTat;

    @Column(name = "quality_score")
    private double qualityScore;

    @Column(name = "sla_met_count")
    private int slaMetCount;

    @Column(name = "sla_breached_count")
    private int slaBreachedCount;

    @Column(name = "denied_claims_count", nullable = false)
    private int deniedClaimsCount;

    @Column(name = "error_rate", nullable = false)
    private double errorRate;

    @Column(name="period")
    private String period;
}