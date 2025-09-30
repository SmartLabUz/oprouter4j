package com.github.dedinc.oprouter4j.exception;

/**
 * Exception thrown when rate limit is exceeded.
 */
public class RateLimitException extends Exception {
    private final Integer retryAfter;

    public RateLimitException(String message) {
        super(message);
        this.retryAfter = null;
    }

    public RateLimitException(String message, Integer retryAfter) {
        super(message);
        this.retryAfter = retryAfter;
    }

    public Integer getRetryAfter() {
        return retryAfter;
    }
}

