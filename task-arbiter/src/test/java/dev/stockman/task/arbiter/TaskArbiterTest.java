package dev.stockman.task.arbiter;

import dev.stockman.retry.Retry;
import dev.stockman.retry.spring7.RetryConfiguration;
import dev.stockman.retry.spring7.SpringRetryTemplateAdapter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;


@DisplayNameGeneration(CamelCaseToSentences.class)
@SpringJUnitConfig(classes = RetryConfiguration.class)
@TestPropertySource(properties = {
        "retry.maxAttempts=2",
        "retry.initialInterval=50",
        "retry.multiplier=2",
        "retry.maxInterval=1000",
        "retry.jitter=10",
        "retry.retryableExceptions=java.lang.RuntimeException",
        "retry.nonRetryableExceptions=java.lang.IllegalArgumentException"
})
public class TaskArbiterTest {

    @Autowired
    private RetryTemplate retryTemplate;

    @Nested
    class RetryableValueOutput {

        @Test
        void testTaskAlreadyCompleted() throws Throwable {
            Retry.RetrySpec retrySpec = new SpringRetryTemplateAdapter(retryTemplate).named("testTaskAlreadyCompleted");

            List<Integer> list = new ArrayList<>(List.of(1));

            Supplier<String> taskToExecute = () -> "Executed task";
            Supplier<List<Integer>> fetchStatus = () -> list;
            Function<List<Integer>, Boolean> checkState = s -> s.size() == 1;
            Consumer<List<Integer>> updateState = s -> s.add(1);

            var result = TaskArbiter.run(retrySpec, taskToExecute, fetchStatus, checkState, updateState);

            Assertions.assertEquals(1, result.status().size());
            Assertions.assertTrue(result.result().isEmpty());
        }

        @Test
        void testTaskNotCompletedThenSkip() throws Throwable {
            Retry.RetrySpec retrySpec = new SpringRetryTemplateAdapter(retryTemplate).named("testTaskNotCompletedThenSkip");

            List<Integer> list = new ArrayList<>(List.of(1));

            Supplier<String> taskToExecute = () -> "Executed task";
            Supplier<List<Integer>> fetchStatus = () -> list;
            Function<List<Integer>, Boolean> checkState = s -> s.size() == 2;
            Consumer<List<Integer>> updateState = s -> {
                s.add(1);
                throw new RuntimeException("Optimistic locking failed");
            };

            var result = TaskArbiter.run(retrySpec, taskToExecute, fetchStatus, checkState, updateState);

            Assertions.assertEquals(2, result.status().size());
            Assertions.assertTrue(result.result().isEmpty());
        }

        @Test
        void testTaskNotCompletedThenExecute() throws Throwable {
            Retry.RetrySpec retrySpec = new SpringRetryTemplateAdapter(retryTemplate).named("testTaskNotCompletedThenExecute");

            List<Integer> list = new ArrayList<>(List.of(1));

            Supplier<String> taskToExecute = () -> "Executed task";
            Supplier<List<Integer>> fetchStatus = () -> list;
            Function<List<Integer>, Boolean> checkState = s -> s.size() == 2;
            Consumer<List<Integer>> updateState = s -> {
                s.add(1);
            };

            var result = TaskArbiter.run(retrySpec, taskToExecute, fetchStatus, checkState, updateState);

            Assertions.assertEquals(2, result.status().size());
            Assertions.assertEquals("Executed task", result.result().get());
        }

        @Test
        void testTaskExecutesOnlyOnce() throws Throwable {
            Retry.RetrySpec retrySpec = new SpringRetryTemplateAdapter(retryTemplate).named("testTaskNotCompletedThenExecute");

            List<Integer> list = new ArrayList<>(List.of(1));
            interface Foobar {
                String foobar();
            }
            Foobar foobar = Mockito.mock(Foobar.class);
            Mockito.when(foobar.foobar()).thenReturn("Executed task");

            Supplier<String> taskToExecute = foobar::foobar;
            Supplier<List<Integer>> fetchStatus = () -> list;
            Function<List<Integer>, Boolean> checkState = s -> s.size() > 1;
            Consumer<List<Integer>> updateState = s -> {
                if (s.size() > 1) {
                    throw new RuntimeException("Precondition failed");
                }
                s.add(1);
            };

            for (int i = 0; i < 5; i++) {
                TaskArbiter.run(retrySpec, taskToExecute, fetchStatus, checkState, updateState);
            }

            Mockito.verify(foobar, Mockito.times(1)).foobar();

        }

    }

    @Nested
    class RetryableVoidOutput {
        @Test
        void testTaskAlreadyCompleted() throws Throwable {
            Retry.RetrySpec retrySpec = new SpringRetryTemplateAdapter(retryTemplate).named("testTaskAlreadyCompleted");

            List<Integer> list = new ArrayList<>(List.of(1));

            Runnable taskToExecute = () -> {};
            Supplier<List<Integer>> fetchStatus = () -> list;
            Function<List<Integer>, Boolean> checkState = s -> s.size() == 1;
            Consumer<List<Integer>> updateState = s -> s.add(1);

            var result = TaskArbiter.run(retrySpec, taskToExecute, fetchStatus, checkState, updateState);

            Assertions.assertEquals(1, result.size());
        }

        @Test
        void testTaskNotCompletedThenSkip() throws Throwable {
            Retry.RetrySpec retrySpec = new SpringRetryTemplateAdapter(retryTemplate).named("testTaskNotCompletedThenSkip");

            List<Integer> list = new ArrayList<>(List.of(1));

            Runnable taskToExecute = () -> {};
            Supplier<List<Integer>> fetchStatus = () -> list;
            Function<List<Integer>, Boolean> checkState = s -> s.size() == 2;
            Consumer<List<Integer>> updateState = s -> {
                s.add(1);
                throw new RuntimeException("Optimistic locking failed");
            };

            var result = TaskArbiter.run(retrySpec, taskToExecute, fetchStatus, checkState, updateState);

            Assertions.assertEquals(2, result.size());
        }

        @Test
        void testTaskNotCompletedThenExecute() throws Throwable {
            Retry.RetrySpec retrySpec = new SpringRetryTemplateAdapter(retryTemplate).named("testTaskNotCompletedThenExecute");

            List<Integer> list = new ArrayList<>(List.of(1));

            Runnable taskToExecute = () -> {};
            Supplier<List<Integer>> fetchStatus = () -> list;
            Function<List<Integer>, Boolean> checkState = s -> s.size() == 2;
            Consumer<List<Integer>> updateState = s -> {
                s.add(1);
            };

            var result = TaskArbiter.run(retrySpec, taskToExecute, fetchStatus, checkState, updateState);

            Assertions.assertEquals(2, result.size());
        }

        @Test
        void testTaskExecutesOnlyOnce() throws Throwable {
            Retry.RetrySpec retrySpec = new SpringRetryTemplateAdapter(retryTemplate).named("testTaskNotCompletedThenExecute");

            List<Integer> list = new ArrayList<>(List.of(1));
            interface Foobar {
                void foobar();
            }
            Foobar foobar = Mockito.mock(Foobar.class);

            Runnable taskToExecute = foobar::foobar;
            Supplier<List<Integer>> fetchStatus = () -> list;
            Function<List<Integer>, Boolean> checkState = s -> s.size() > 1;
            Consumer<List<Integer>> updateState = s -> {
                if (s.size() > 1) {
                    throw new RuntimeException("Precondition failed");
                }
                s.add(1);
            };

            for (int i = 0; i < 5; i++) {
                TaskArbiter.run(retrySpec, taskToExecute, fetchStatus, checkState, updateState);
            }

            Mockito.verify(foobar, Mockito.times(1)).foobar();

        }
    }
}