package com.claiminsight.metrics.exception;

public class ResourceNotFoundException extends RuntimeException {
    
    /** Constructs the exception with a descriptive message. */
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
