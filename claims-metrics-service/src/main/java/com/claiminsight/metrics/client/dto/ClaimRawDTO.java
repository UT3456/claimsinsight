package com.claiminsight.metrics.client.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO received from data-ingestion-service via Feign.
 * Mirrors IngestionResponseDTO from the ingestion service.
 */
@Data
public class ClaimRawDTO {

    private Long rawId;
    private String claimId;
    private Long feedId;
    private String feedType;
    private String payloadJson;
    private LocalDateTime ingestedDate;
}
