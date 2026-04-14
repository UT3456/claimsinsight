package com.claims.entity;

import com.claims.enums.AgingBucket;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "aging_record")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AgingRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "aging_id")
    private Long agingId;

    @Column(name = "claim_id", nullable = false)
    private String claimId;

    @Column(name = "aging_days", nullable = false)
    private Integer agingDays;

    @Enumerated(EnumType.STRING)
    @Column(name = "aging_bucket", nullable = false)
    private AgingBucket agingBucket;
}
