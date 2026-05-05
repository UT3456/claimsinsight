package com.claim360.fraudrisk.entity;

import com.claim360.fraudrisk.enums.InvestigationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Fraud investigation opened from a RiskScore by a FRAUD_ANALYST clicking
 * "Escalate SIU" on a flagged claim. One row per escalation.
 */
@Entity
@Table(name = "investigations", indexes = {
    @Index(name = "idx_inv_claim_id",    columnList = "claim_id"),
    @Index(name = "idx_inv_status",      columnList = "status"),
    @Index(name = "idx_inv_assigned_to", columnList = "assigned_to"),
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Investigation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "investigation_id")
    private Long investigationId;

    @Column(name = "claim_id", nullable = false, length = 100)
    private String claimId;

    /** Reference to the RiskScore.scoreId that triggered the escalation. */
    @Column(name = "risk_score_id")
    private Long riskScoreId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private InvestigationStatus status = InvestigationStatus.NEW;

    /** Optional — username of the SIU/Fraud manager assigned to this case. */
    @Column(name = "assigned_to", length = 100)
    private String assignedTo;

    /** Username of the analyst who escalated. */
    @Column(name = "opened_by", length = 100, nullable = false, updatable = false)
    private String openedBy;

    @Column(name = "opened_at", nullable = false, updatable = false)
    private LocalDateTime openedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "notes", length = 2000)
    private String notes;

    @PrePersist
    public void onCreate() {
        if (openedAt == null) openedAt = LocalDateTime.now();
        if (status == null)   status   = InvestigationStatus.NEW;
    }
}
