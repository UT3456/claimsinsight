package com.claiminsight.metrics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

/** Standard error response body returned by the GlobalExceptionHandler. */
@Data
@AllArgsConstructor
public class ErrorResponseDTO {
    
    private LocalDateTime timestamp;
    
    private int status;
    
    private String error;
    
    private String message;
}
