package com.claiminsight.ingestion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Request body DTO for updating the status of a data feed. */
@Data
public class FeedStatusUpdateDTO {

    @NotBlank(message = "{NotBlank.feedStatusUpdateDTO.status}")
    @Size(max = 20, message = "{Size.feedStatusUpdateDTO.status}")
    @Pattern(regexp = "^[a-zA-Z]+$", message = "{Pattern.feedStatusUpdateDTO.status}")
    private String status;
}
