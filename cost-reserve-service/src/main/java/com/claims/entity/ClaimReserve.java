package com.claims.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "claim_reserve")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClaimReserve {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reserve_id")
    private Long reserveId;

    @Column(name = "claim_id", nullable = false)
    private String claimId;

    @Column(name = "reserve_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal reserveAmount;

    @Column(name = "updated_date", nullable = false)
    private LocalDate updatedDate;
}
