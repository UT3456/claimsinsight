package com.claims.entity;

import com.claims.enums.CostType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "claim_cost")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClaimCost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cost_id")
    private Long costId;

    @Column(name = "claim_id", nullable = false)
    private String claimId;

    @Enumerated(EnumType.STRING)
    @Column(name = "cost_type", nullable = false)
    private CostType costType;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "cost_date", nullable = false)
    private LocalDate costDate;
}
