package dev.stockman.task.arbiter;

import dev.stockman.retry.Retry;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * TaskArbiter ensures a task is executed exactly once by coordinating with an external state.
 * It uses a {@link Retry.RetrySpec} to handle concurrency conflicts (like optimistic locking failures).
 */
public final class TaskArbiter {

    private TaskArbiter() {
    }

    /**
     * Internal state holder to track if a task was already completed.
     */
    record State<S>(boolean alreadyDone, S currentStatus){}

    /**
     * Result container for the arbiter execution.
     * @param result The result of the task, present only if the task was actually executed.
     * @param status The final status retrieved from the state provider.
     */
    public record TaskResult<T, S>(Optional<T> result, S status) {
    }

    /**
     * Executes a task that returns a value, provided the state check passes.
     *
     * @param retry       The retry specification used to handle state update conflicts.
     * @param task        The logic to execute if the state is not "done".
     * @param fetchStatus A supplier to retrieve the current state from a persistent store.
     * @param checkState  A predicate that returns true if the status indicates the task is already done.
     * @param updateState A consumer to mark the status as "done" in the persistent store.
     * @return A {@link TaskResult} containing the task output (if run) and the current status.
     * @throws Throwable If retries are exhausted or the task fails.
     */
    public static <T, S> TaskResult<T, S> run(Retry.RetrySpec retry, Supplier<T> task, Supplier<S> fetchStatus, Function<S, Boolean> checkState, Consumer<S> updateState) throws Throwable {
        var state = retry.call(() -> alreadyDone(fetchStatus, checkState, updateState)).execute();
        if (state.alreadyDone()) {
            return new TaskResult<>(Optional.empty(), state.currentStatus());
        } else {
            return new TaskResult<>(Optional.ofNullable(task.get()), state.currentStatus());
        }
    }

    /**
     * Executes a void task, provided the state check passes.
     *
     * @param retry       The retry specification used to handle state update conflicts.
     * @param task        The logic to execute if the state is not "done".
     * @param fetchStatus A supplier to retrieve the current state from a persistent store.
     * @param checkState  A predicate that returns true if the status indicates the task is already done.
     * @param updateState A consumer to mark the status as "done" in the persistent store.
     * @return The current status after the check or task execution.
     * @throws Throwable If retries are exhausted or the task fails.
     */
    public static <S> S run(Retry.RetrySpec retry, Runnable task, Supplier<S> fetchStatus, Function<S, Boolean> checkState, Consumer<S> updateState) throws Throwable {
        var state = retry.call(() -> alreadyDone(fetchStatus, checkState, updateState)).execute();
        if (!state.alreadyDone()) {
            task.run();
        }
        return state.currentStatus();
    }

    private static <S> State<S> alreadyDone(Supplier<S> fetchStatus, Function<S, Boolean> checkState, Consumer<S> updateState) {
        final S currentStatus = fetchStatus.get();
        if (checkState.apply(currentStatus)) {
            return new State<>(true, currentStatus);
        } else {
            updateState.accept(currentStatus);
            return new State<>(false, currentStatus);
        }
    }


}
