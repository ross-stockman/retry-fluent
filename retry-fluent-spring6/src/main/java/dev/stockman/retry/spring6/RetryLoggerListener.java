package dev.stockman.retry.spring6;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.RetryPolicy;

import java.util.List;

class RetryLoggerListener implements RetryListener {

    private static final Logger log = LoggerFactory.getLogger(RetryLoggerListener.class);

    private final long maxAttempts;
    private final List<Class<? extends Throwable>> nonRetryableExceptions;

    RetryLoggerListener(RetryPolicy retryPolicy, List<Class<? extends Throwable>> nonRetryableExceptions) {
        this.maxAttempts = retryPolicy.getMaxAttempts();
        this.nonRetryableExceptions = nonRetryableExceptions;
    }

    @Override
    public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
        String name = (String) context.getAttribute(SpringRetryTemplateAdapter.CONTEXT_NAME_KEY);
        int attempts = context.getRetryCount(); // In Spring 6, getRetryCount() is incremented before onError

        log.info("Try attempt {}/{} failed. Last exception: {} -- {}", attempts, maxAttempts, throwable, name);
    }

    @Override
    public <T, E extends Throwable> void onSuccess(RetryContext context, RetryCallback<T, E> callback, T result) {
        String name = (String) context.getAttribute(SpringRetryTemplateAdapter.CONTEXT_NAME_KEY);
        int attempts = context.getRetryCount() + 1;
        log.info("Try attempt {}/{} succeeded. -- {}", attempts, maxAttempts, name);
    }

    @Override
    public <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
        if (throwable != null) {
            String name = (String) context.getAttribute(SpringRetryTemplateAdapter.CONTEXT_NAME_KEY);
            int attempts = context.getRetryCount();

            // Direct check: Is this specific exception type in our "do not retry" list?
            boolean isExplicitlyNonRetryable = nonRetryableExceptions.stream()
                    .anyMatch(type -> type.isInstance(throwable));

            if (isExplicitlyNonRetryable) {
                log.info("Retry policy terminated after {}/{} attempts failed. Non-retryable exception encountered: {} -- {}", attempts, maxAttempts, throwable, name);
            } else {
                log.info("Retry policy exhausted after {}/{} max attempts failed. Last exception: {} -- {}", attempts, maxAttempts, throwable, name);
            }
        }
    }
}