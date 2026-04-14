package com.claiminsight.ingestion.dto;

import lombok.Data;

import java.time.LocalDateTime;

/** Response DTO returned after creating or fetching a data feed. */
@Data
public class DataFeedResponseDTO {
    
    private Long feedId;
    
    private String feedType;
    
    private String sourceSystem;
    
    private LocalDateTime lastSyncDate;
    
    private String status;
    
    private LocalDateTime createdDate;
}
