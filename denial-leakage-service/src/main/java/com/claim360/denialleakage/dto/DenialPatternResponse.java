package com.claim360.denialleakage.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DenialPatternResponse {

    private Long patternId;
    private String claimId;
    private String denialCode;
    private String reason;
    private LocalDate occurrenceDate;
}