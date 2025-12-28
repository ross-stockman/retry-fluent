package dev.stockman.retry;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Entry point for the fluent retry API. This interface abstracts the underlying
 * retry mechanism (e.g., Spring Retry) to provide a stable API during framework migrations.
 */
public interface Retry {

    /**
     * Starts a retry specification with a specific name.
     * The name is used for logging, metrics, and debugging context.
     *
     * @param operationName The descriptive name of the operation being retried.
     * @return A specification to continue building the retry task.
     */
    RetrySpec named(String operationName);

    /**
     * Starts a retry specification without a specific name.
     * Use this for simple or internal operations where context is not required.
     *
     * @return A specification to continue building the retry task.
     */
    RetrySpec anonymous();

    /**
     * A specification that defines the action to be retried.
     */
    interface RetrySpec {
        /**
         * Defines a retryable action that returns a value.
         *
         * @param action The code to execute within the retry context.
         * @param <R>    The type of the result returned by the action.
         * @return A specification to finalize execution or add a fallback.
         */
        <R> CallSpec<R> call(Supplier<R> action);

        /**
         * Defines a retryable action that performs a void operation.
         *
         * @param action The code to execute within the retry context.
         * @return A specification to finalize execution or add a fallback.
         */
        RunSpec run(Runnable action);
    }

    /**
     * Finalizing specification for operations that return a value.
     *
     * @param <R> The type of the result.
     */
    interface CallSpec<R> {
        /**
         * Executes the retryable action and returns the result.
         * If the retry policy is exhausted, the last exception encountered will be thrown.
         *
         * @return The result of the successful execution.
         * @throws Throwable or the specific error encountered during the final attempt.
         */
        R execute() throws Throwable;

        /**
         * Executes the retryable action. If the retry policy is exhausted,
         * the provided fallback function is invoked to provide a default value.
         *
         * @param fallback A function that accepts the final exception and returns a result of type {@code R}.
         * @return The result of the action, or the result of the fallback if retries failed.
         */
        R fallback(Function<Throwable, R> fallback);
    }

    /**
     * Finalizing specification for void operations.
     */
    interface RunSpec {
        /**
         * Executes the retryable action.
         * If the retry policy is exhausted, the last exception encountered will be thrown.
         *
         * @throws Throwable or the specific error encountered during the final attempt.
         */
        void execute() throws Throwable;

        /**
         * Executes the retryable action. If the retry policy is exhausted,
         * the provided fallback consumer is invoked to handle the final exception.
         *
         * @param fallback A consumer that accepts the final exception.
         */
        void fallback(Consumer<Throwable> fallback);
    }
}
