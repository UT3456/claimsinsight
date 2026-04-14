package com.claiminsight.ingestion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Request body DTO for registering or updating a data feed source. */
@Data
public class DataFeedRequestDTO {

    @NotBlank(message = "{NotBlank.dataFeedRequestDTO.feedType}")
    @Size(max = 20, message = "{Size.dataFeedRequestDTO.feedType}")
    @Pattern(regexp = "^[a-zA-Z]+$", message = "{Pattern.dataFeedRequestDTO.feedType}")
    private String feedType;

    @NotBlank(message = "{NotBlank.dataFeedRequestDTO.sourceSystem}")
    @Size(min = 2, max = 100, message = "{Size.dataFeedRequestDTO.sourceSystem}")
    private String sourceSystem;

    @NotBlank(message = "{NotBlank.dataFeedRequestDTO.status}")
    @Size(max = 20, message = "{Size.dataFeedRequestDTO.status}")
    @Pattern(regexp = "^[a-zA-Z]+$", message = "{Pattern.dataFeedRequestDTO.status}")
    private String status;
}
