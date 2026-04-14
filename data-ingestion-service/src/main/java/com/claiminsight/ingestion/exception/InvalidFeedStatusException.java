package com.claiminsight.ingestion.exception;

/** Thrown when a claim is ingested into a feed that is not ACTIVE. */
public class InvalidFeedStatusException extends RuntimeException {

    /** Constructs the exception with a descriptive message. */
    public InvalidFeedStatusException(String message) {
        super(message);
    }
}
