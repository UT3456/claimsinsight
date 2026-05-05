package com.claim360.denialleakage.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "denial_pattern", indexes = {
    @Index(name = "idx_dp_claim_id",        columnList = "claim_id"),
    @Index(name = "idx_dp_denial_code",     columnList = "denial_code"),
    @Index(name = "idx_dp_occurrence_date", columnList = "occurrence_date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DenialPattern {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pattern_id")
    private Long patternId;

    @Column(name = "claim_id", nullable = false)
    private String claimId;

    @Column(name = "denial_code", nullable = false)
    private String denialCode;

    @Column(name = "reason", nullable = false)
    private String reason;

    @Column(name = "occurrence_date", nullable = false)
    private LocalDate occurrenceDate;
}