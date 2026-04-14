package com.claims.dto.response;

import com.claims.enums.AgingBucket;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AgingRecordResponse {

    private Long agingId;
    private String claimId;
    private Integer agingDays;
    private AgingBucket agingBucket;
}
