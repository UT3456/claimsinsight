package com.demo.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Client-side DTO mirroring AgingRecordResponse from cost-reserve-service.
 * agingBucket is a String (enum name, e.g. "BUCKET_90_PLUS") to avoid
 * a hard dependency on the remote service's enum type.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AgingRecordDTO {

    private Long agingId;
    private String claimId;
    private Integer agingDays;
    private String agingBucket;
}
