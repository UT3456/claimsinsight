package com.demo.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

/**
 * MOCK TABLE: aging_records
 */
@Entity
@Table(name = "aging_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgingRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "aging_id")
    private Long agingId;

    @Column(name = "claim_id", length = 50)
    private String claimId;

    @Column(name = "aging_days")
    private Integer agingDays;

    @Column(name = "aging_bucket", length = 20)
    private String agingBucket;   // "0-30" / "31-60" / "61-90" / "91-120" / "120+"

    @Column(name = "as_of_date")
    private LocalDate asOfDate;

    @Column(name = "status", length = 30)
    private String status;   // OPEN / PENDING / IN_REVIEW / REOPENED
}