package com.claiminsight.metrics.exception;

import com.claiminsight.metrics.dto.ErrorResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

/** Centralized exception handler that converts exceptions into consistent JSON error responses. */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    /** Handles ResourceNotFoundException — returns 404. */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleResourceNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new ErrorResponseDTO(LocalDateTime.now(), HttpStatus.NOT_FOUND.value(),
                        "Not Found", ex.getMessage())
        );
    }
    
    /** Handles InvalidMetricException — returns 400. */
    @ExceptionHandler(InvalidMetricException.class)
    public ResponseEntity<ErrorResponseDTO> handleInvalidMetric(InvalidMetricException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ErrorResponseDTO(LocalDateTime.now(), HttpStatus.BAD_REQUEST.value(),
                        "Bad Request", ex.getMessage())
        );
    }
    
    /** Handles @Valid failures — collects all field errors and returns 400. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidationErrors(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ErrorResponseDTO(LocalDateTime.now(), HttpStatus.BAD_REQUEST.value(),
                        "Bad Request", message)
        );
    }
    
    /** Handles all other unexpected exceptions — returns 500. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGenericException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ErrorResponseDTO(LocalDateTime.now(), HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "Internal Server Error", "An unexpected error occurred: " + ex.getMessage())
        );
    }
}
