package com.demo.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

/**
 * Persistence entity representing the 'sla_violation' table.
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@Entity
@Table(name = "sla_violation")
public class SLAViolation {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "violation_id")
    private Long violationId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "claim_id", referencedColumnName = "claim_id")
    private Claim claim;

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