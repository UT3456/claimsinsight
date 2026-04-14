package com.demo.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * MOCK TABLE: risk_scores
 */
@Entity
@Table(name = "risk_scores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "score_id")
    private Long scoreId;

    @Column(name = "claim_id", length = 50)
    private String claimId;

    @Column(name = "score_value")
    private Integer scoreValue;

    @Column(name = "risk_level", length = 20)
    private String riskLevel;

    @Column(name = "computed_date")
    private LocalDateTime computedDate;
}
