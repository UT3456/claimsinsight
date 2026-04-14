package com.claiminsight.ingestion.dto;

import lombok.Data;

import java.time.LocalDateTime;

/** Response DTO returned after ingesting a raw claim record. */
@Data
public class IngestionResponseDTO {
    
    private Long rawId;
    
    private String claimId;
    
    private Long feedId;
    
    private String feedType;
    
    private String payloadJson;
    
    private LocalDateTime ingestedDate;
}
