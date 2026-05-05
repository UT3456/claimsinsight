package com.claim360.fraudrisk.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "risk_score", indexes = {
    @Index(name = "idx_rs_claim_id",      columnList = "claim_id"),
    @Index(name = "idx_rs_score_value",   columnList = "score_value"),
    @Index(name = "idx_rs_computed_date", columnList = "computed_date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RiskScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "score_id")
    private Long scoreId;

    @Column(name = "claim_id", nullable = false)
    private String claimId;

    @Column(name = "score_value", nullable = false)
    private Double scoreValue;

    @Column(name = "computed_date", nullable = false)
    private LocalDate computedDate;
}