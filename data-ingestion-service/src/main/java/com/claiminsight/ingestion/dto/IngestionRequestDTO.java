package com.claiminsight.ingestion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Request body DTO for ingesting a raw claim payload. */
@Data
public class IngestionRequestDTO {

    @NotBlank(message = "{NotBlank.ingestionRequestDTO.claimId}")
    @Size(min = 3, max = 100, message = "{Size.ingestionRequestDTO.claimId}")
    @Pattern(regexp = "^[a-zA-Z0-9\\-_]+$", message = "{Pattern.ingestionRequestDTO.claimId}")
    private String claimId;

    @NotNull(message = "{NotNull.ingestionRequestDTO.feedId}")
    @Positive(message = "{Positive.ingestionRequestDTO.feedId}")
    private Long feedId;

    @NotBlank(message = "{NotBlank.ingestionRequestDTO.payloadJson}")
    private String payloadJson;
}
