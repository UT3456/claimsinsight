package com.claiminsight.ingestion.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/** JPA entity representing a raw claim JSON payload stored in the claim_raw table. */
@Entity
@Table(name = "claim_raw", indexes = {
    @Index(name = "idx_claim_raw_claim_id", columnList = "claim_id")
})
@Getter @Setter @NoArgsConstructor
@ToString(exclude = "dataFeed")
public class ClaimRaw {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "raw_id")
    private Long rawId;

    @Column(name = "claim_id", nullable = false, length = 100)
    private String claimId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feed_id", nullable = false)
    private DataFeed dataFeed;

    @Column(name = "payload_json", nullable = false, columnDefinition = "LONGTEXT")
    private String payloadJson;

    @Column(name = "ingested_date", nullable = false, updatable = false)
    private LocalDateTime ingestedDate;

    /** Sets ingestedDate to the current timestamp before the first INSERT. */
    @PrePersist
    public void onPrePersist() {
        this.ingestedDate = LocalDateTime.now();
    }
}
