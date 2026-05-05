package com.claiminsight.gateway.exception;

/** Thrown when a login is blocked due to too many consecutive failed attempts. */
public class AccountLockedException extends RuntimeException {
    public AccountLockedException(String message) {
        super(message);
    }
}
