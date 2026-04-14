package com.claiminsight.metrics.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Request body DTO for recording a new KPI metric. */
@Data
public class ClaimKpiRequestDTO {

    @NotBlank(message = "{NotBlank.claimKpiRequestDTO.claimId}")
    @Size(min = 3, max = 100, message = "{Size.claimKpiRequestDTO.claimId}")
    @Pattern(regexp = "^[a-zA-Z0-9\\-_]+$", message = "{Pattern.claimKpiRequestDTO.claimId}")
    private String claimId;

    @NotBlank(message = "{NotBlank.claimKpiRequestDTO.metricName}")
    @Size(max = 50, message = "{Size.claimKpiRequestDTO.metricName}")
    @Pattern(regexp = "^[a-zA-Z0-9_ ]+$", message = "{Pattern.claimKpiRequestDTO.metricName}")
    private String metricName;

    @NotNull(message = "{NotNull.claimKpiRequestDTO.metricValue}")
    @DecimalMin(value = "0.0", message = "{DecimalMin.claimKpiRequestDTO.metricValue}")
    private BigDecimal metricValue;

    @NotNull(message = "{NotNull.claimKpiRequestDTO.metricDate}")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate metricDate;
}
