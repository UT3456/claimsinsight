package com.claim360.denialleakage.dto;

import com.claim360.denialleakage.enums.LeakageType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeakageFlagResponse {

    private Long leakageId;
    private String claimId;
    private LeakageType leakageType;
    private Double estimatedLoss;
    private LocalDate identifiedDate;
}