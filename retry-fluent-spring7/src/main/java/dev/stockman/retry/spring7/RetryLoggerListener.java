package dev.stockman.retry.spring7;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.retry.*;
import org.springframework.util.backoff.ExponentialBackOff;

class RetryLoggerListener implements RetryListener {

    private static final Logger log = LoggerFactory.getLogger(RetryLoggerListener.class);

    private final long maxAttempts;

    RetryLoggerListener(ExponentialBackOff backOffPolicy) {
        long maxRetries = backOffPolicy.getMaxAttempts();
        this.maxAttempts = maxRetries + 1;
    }
    public void onRetryableExecution(@NonNull RetryPolicy retryPolicy, @NonNull Retryable<?> retryable, RetryState retryState) {
        int attempts = retryState.getRetryCount() + 1;
        if (!retryState.isSuccessful()) {
            log.info("Try attempt {}/{} failed. Last exception: {} -- {}", attempts, maxAttempts, retryState.getLastException(), retryable.getName());
        } else {
            log.info("Try attempt {}/{} succeeded. -- {}", attempts, maxAttempts, retryable.getName());
        }
    }
    public void onRetryPolicyExhaustion(RetryPolicy retryPolicy, @NonNull Retryable<?> retryable, RetryException exception) {
        int attempts = exception.getRetryCount() + 1;
        boolean shouldRetry = retryPolicy.shouldRetry(exception.getCause());
        if (shouldRetry) {
            log.info("Retry policy exhausted after {}/{} max attempts failed. Last exception: {} -- {}", attempts, maxAttempts, exception.getCause(), retryable.getName());
        } else {
            log.info("Retry policy terminated after {}/{} attempts failed. Non-retryable exception encountered: {} -- {}", attempts, maxAttempts, exception.getCause(), retryable.getName());
        }
    }

}
