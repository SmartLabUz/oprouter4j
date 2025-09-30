package com.github.dedinc.oprouter4j.util;

import com.github.dedinc.oprouter4j.core.Config;
import com.github.dedinc.oprouter4j.core.Logger;
import com.github.dedinc.oprouter4j.exception.RateLimitException;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;

import java.io.IOException;
import java.util.function.Supplier;

/**
 * Retry handler using Resilience4j (Tenacity equivalent for Java).
 * Provides intelligent retry logic with exponential backoff.
 */
public class RetryHandler {
    private static final Logger logger = Logger.getLogger(RetryHandler.class);

    private final Retry retry;
    private final Config config;

    public RetryHandler(Config config) {
        this.config = config;
        this.retry = createRetry();
    }

    public RetryHandler() {
        this(Config.load());
    }

    private Retry createRetry() {
        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(config.getMaxRetries())
                .intervalFunction(attempt -> {
                    // Exponential backoff with jitter
                    double exponentialDelay = config.getBaseDelay() * Math.pow(2, attempt - 1);
                    double jitter = Math.random() * 5;
                    long delayMs = (long) (Math.min(exponentialDelay + jitter, config.getMaxDelay()) * 1000);
                    return delayMs;
                })
                .retryOnException(e -> {
                    // Unwrap RuntimeException to check the cause
                    Throwable cause = e;
                    if (e instanceof RuntimeException && e.getCause() != null) {
                        cause = e.getCause();
                    }

                    // Retry on rate limit and IO exceptions
                    if (cause instanceof RateLimitException) {
                        RateLimitException rle = (RateLimitException) cause;
                        // Only show warning, not error
                        logger.warning("Rate limited. Retry after " + rle.getRetryAfter() + " seconds");
                        return true;
                    }
                    if (cause instanceof IOException) {
                        // Only show warning, not error
                        logger.warning("Network error: " + cause.getMessage());
                        return true;
                    }
                    return false;
                })
                .retryExceptions(RateLimitException.class, IOException.class)
                .ignoreExceptions(IllegalArgumentException.class)
                .build();

        RetryRegistry registry = RetryRegistry.of(retryConfig);
        Retry retry = registry.retry("openrouter-api");

        // Add event listeners for logging (but suppress spam)
        retry.getEventPublisher()
                .onRetry(event -> {
                    // Only log at debug level to prevent spam
                    logger.debug("Retry attempt " + event.getNumberOfRetryAttempts() +
                            " after " + event.getWaitInterval().toMillis() + "ms");
                })
                .onSuccess(event -> {
                    if (event.getNumberOfRetryAttempts() > 0) {
                        logger.info("Request succeeded after " + event.getNumberOfRetryAttempts() + " retries");
                    }
                })
                .onError(event -> {
                    logger.error("Request failed after " + event.getNumberOfRetryAttempts() + " retries");
                });

        return retry;
    }

    /**
     * Execute a supplier with retry logic.
     */
    public <T> T execute(Supplier<T> supplier) {
        Supplier<T> decoratedSupplier = Retry.decorateSupplier(retry, supplier);
        return decoratedSupplier.get();
    }

    /**
     * Execute a runnable with retry logic.
     */
    public void execute(Runnable runnable) {
        Runnable decoratedRunnable = Retry.decorateRunnable(retry, runnable);
        decoratedRunnable.run();
    }

    /**
     * Get the underlying Retry instance for advanced usage.
     */
    public Retry getRetry() {
        return retry;
    }

    /**
     * Get retry statistics.
     */
    public RetryStats getStats() {
        Retry.Metrics metrics = retry.getMetrics();
        return new RetryStats(
                metrics.getNumberOfSuccessfulCallsWithoutRetryAttempt(),
                metrics.getNumberOfSuccessfulCallsWithRetryAttempt(),
                metrics.getNumberOfFailedCallsWithoutRetryAttempt(),
                metrics.getNumberOfFailedCallsWithRetryAttempt()
        );
    }

    /**
     * Retry statistics holder.
     */
    public static class RetryStats {
        private final long successWithoutRetry;
        private final long successWithRetry;
        private final long failedWithoutRetry;
        private final long failedWithRetry;

        public RetryStats(long successWithoutRetry, long successWithRetry,
                          long failedWithoutRetry, long failedWithRetry) {
            this.successWithoutRetry = successWithoutRetry;
            this.successWithRetry = successWithRetry;
            this.failedWithoutRetry = failedWithoutRetry;
            this.failedWithRetry = failedWithRetry;
        }

        public long getSuccessWithoutRetry() {
            return successWithoutRetry;
        }

        public long getSuccessWithRetry() {
            return successWithRetry;
        }

        public long getFailedWithoutRetry() {
            return failedWithoutRetry;
        }

        public long getFailedWithRetry() {
            return failedWithRetry;
        }

        public long getTotalSuccess() {
            return successWithoutRetry + successWithRetry;
        }

        public long getTotalFailed() {
            return failedWithoutRetry + failedWithRetry;
        }

        public long getTotalCalls() {
            return getTotalSuccess() + getTotalFailed();
        }

        @Override
        public String toString() {
            return String.format(
                    "RetryStats{total=%d, success=%d (without retry: %d, with retry: %d), " +
                            "failed=%d (without retry: %d, with retry: %d)}",
                    getTotalCalls(), getTotalSuccess(), successWithoutRetry, successWithRetry,
                    getTotalFailed(), failedWithoutRetry, failedWithRetry
            );
        }
    }
}

