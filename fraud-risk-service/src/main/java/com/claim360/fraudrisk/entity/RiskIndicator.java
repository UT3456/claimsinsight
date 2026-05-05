package com.claim360.fraudrisk.entity;

import com.claim360.fraudrisk.enums.IndicatorType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "risk_indicator", indexes = {
    @Index(name = "idx_ri_claim_id",   columnList = "claim_id"),
    @Index(name = "idx_ri_claim_type", columnList = "claim_id,indicator_type")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RiskIndicator {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "indicator_id")
    private Long indicatorId;

    @Column(name = "claim_id", nullable = false)
    private String claimId;

    @Enumerated(EnumType.STRING)
    @Column(name = "indicator_type", nullable = false)
    private IndicatorType indicatorType;

    @Column(name = "severity", nullable = false)
    private String severity;

    @Column(name = "triggered_date", nullable = false)
    private LocalDate triggeredDate;
}