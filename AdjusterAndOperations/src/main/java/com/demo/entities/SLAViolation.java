package com.demo.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

/**
 * Persistence entity representing the 'sla_violation' table.
 *
 * <p>{@code claimId} is a soft reference to the {@code claim} table owned by
 * claims-metrics-service. We deliberately do NOT model a JPA {@code @ManyToOne}
 * relationship here because the {@code claim} table lives in another service
 * and should be reached via the Feign client, not through a cross-module FK.
 * This mirrors how every other microservice in the platform refers to claims.</p>
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@Entity
@Table(name = "sla_violation", indexes = {
    @Index(name = "idx_sla_adjuster_id",    columnList = "adjuster_id"),
    @Index(name = "idx_sla_violation_date", columnList = "violation_date")
})
public class SLAViolation {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "violation_id")
    private Long violationId;

    @Column(name = "claim_id")
    private Long claimId;

    @Column(name = "adjuster_id", nullable = false)
    private Long adjusterId;

    @Column(name = "violation_type", nullable = false)
    private String violationType;

    @Column(name = "sla_target_days", nullable = false)
    private int slaTargetDays;

    @Column(name = "actual_days", nullable = false)
    private int actualDays;

    @Column(name = "violation_date", nullable = false)
    @Temporal(TemporalType.DATE)
    private Date violationDate;
}
