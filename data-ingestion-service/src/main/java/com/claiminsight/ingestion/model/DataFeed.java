package com.claiminsight.ingestion.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** JPA entity representing an external data feed source stored in the data_feed table. */
@Entity
@Table(name = "data_feed")
@Getter @Setter @NoArgsConstructor
@ToString(exclude = "claimRawList")
public class DataFeed {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "feed_id")
    private Long feedId;

    @Enumerated(EnumType.STRING)
    @Column(name = "feed_type", nullable = false)
    private FeedType feedType;

    @Column(name = "source_system", nullable = false, length = 100)
    private String sourceSystem;

    @Column(name = "last_sync_date")
    private LocalDateTime lastSyncDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private FeedStatus status;

    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @OneToMany(mappedBy = "dataFeed", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<ClaimRaw> claimRawList = new ArrayList<>();

    /** Sets createdDate to the current timestamp before the first INSERT. */
    @PrePersist
    public void onPrePersist() {
        this.createdDate = LocalDateTime.now();
    }
}
