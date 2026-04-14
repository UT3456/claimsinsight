package com.demo.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

/**
 * Persistence entity representing a formal request by a policyholder for
 * coverage or compensation for a covered loss.
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@Entity
@Table(name = "claim")
public class Claim {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "claim_id")
    private Long claimId;

    @Column(name = "policy_id")
    private Long policyId;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "claim_type")
    private String claimType;

    @Column(name = "claim_status")
    private String claimStatus;

    @Column(name = "reported_date")
    @Temporal(TemporalType.DATE)
    private Date reportedDate;

    @Column(name = "settled_date")
    @Temporal(TemporalType.DATE)
    private Date settledDate;

    @Column(name = "amount")
    private Double amount;
}