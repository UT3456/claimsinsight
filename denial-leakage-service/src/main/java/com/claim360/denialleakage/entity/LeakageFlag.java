package com.claim360.denialleakage.entity;

import com.claim360.denialleakage.enums.LeakageType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "leakage_flag", indexes = {
    @Index(name = "idx_lf_claim_id",     columnList = "claim_id"),
    @Index(name = "idx_lf_leakage_type", columnList = "leakage_type")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeakageFlag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "leakage_id")
    private Long leakageId;

    @Column(name = "claim_id", nullable = false)
    private String claimId;

    @Enumerated(EnumType.STRING)
    @Column(name = "leakage_type", nullable = false)
    private LeakageType leakageType;

    @Column(name = "estimated_loss", nullable = false)
    private Double estimatedLoss;

    @Column(name = "identified_date", nullable = false)
    private LocalDate identifiedDate;
}