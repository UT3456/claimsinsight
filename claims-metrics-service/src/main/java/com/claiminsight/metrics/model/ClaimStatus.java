package com.claiminsight.metrics.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * JPA entity persisting the ACTIVE/INACTIVE state of a claim, keyed by claim_id.
 * One row per claim.
 */
@Entity
@Table(name = "claim_status")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ClaimStatus {

    @Id
    @Column(name = "claim_id", length = 100, nullable = false)
    private String claimId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private ClaimStatusValue status;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        this.updatedAt = Instant.now();
    }

    public enum ClaimStatusValue { ACTIVE, INACTIVE }
}
