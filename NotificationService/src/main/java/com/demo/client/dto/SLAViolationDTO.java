package com.demo.client.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;

/**
 * Client-side DTO mirroring SLAViolationDTO from AdjusterAndOperations service.
 * violationDate is mapped as LocalDate (ISO yyyy-MM-dd); null is handled
 * gracefully in filtering logic.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SLAViolationDTO {

    private Long violationId;
    private Long adjusterId;
    private String violationType;
    private String severity;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate violationDate;
}
