package com.demo.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

/**
 * MOCK TABLE: sla_violations
 */
@Entity
@Table(name = "sla_violations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SlaViolation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "violation_id")
    private Long violationId;

    @Column(name = "claim_id", length = 50)
    private String claimId;

    @Column(name = "adjuster_id")
    private Long adjusterId;

    @Column(name = "violation_type", length = 80)
    private String violationType;

    @Column(name = "sla_target_days")
    private Integer slaTargetDays;

    @Column(name = "actual_days")
    private Integer actualDays;

    @Column(name = "days_overdue")
    private Integer daysOverdue;

    @Column(name = "violation_date")
    private LocalDate violationDate;
}
