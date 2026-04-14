package com.claiminsight.ingestion.exception;

public class InvalidFeedTypeException extends RuntimeException {
    
    /** Constructs the exception with a descriptive message. */
    public InvalidFeedTypeException(String message) {
        super(message);
    }
}
