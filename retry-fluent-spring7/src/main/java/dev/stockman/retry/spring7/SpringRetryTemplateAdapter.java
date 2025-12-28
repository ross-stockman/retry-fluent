package dev.stockman.retry.spring7;

import dev.stockman.retry.Retry;
import org.springframework.core.retry.RetryException;
import org.springframework.core.retry.RetryTemplate;

import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class SpringRetryTemplateAdapter implements Retry {

    private final RetryTemplate retryTemplate;

    public SpringRetryTemplateAdapter(RetryTemplate retryTemplate) {
        this.retryTemplate = retryTemplate;
    }

    @Override
    public RetrySpec named(String operationName) {
        return new SpringRetrySpec(operationName);
    }

    @Override
    public RetrySpec anonymous() {
        return new SpringRetrySpec(UUID.randomUUID().toString());
    }

    private class SpringRetrySpec implements RetrySpec {

        private final String operationName;

        SpringRetrySpec(String operationName) {
            this.operationName = operationName;
        }

        @Override
        public <R> CallSpec<R> call(Supplier<R> action) {
            return new SpringCallSpec<>(operationName, action);
        }

        @Override
        public RunSpec run(Runnable action) {
            return new SpringRunSpec(operationName, action);
        }
    }

    private class SpringCallSpec<R> implements CallSpec<R> {
        private final String name;
        private final Supplier<R> action;

        SpringCallSpec(String name, Supplier<R> action) {
            this.name = name;
            this.action = action;
        }

        @Override
        public R execute() throws Throwable {
            try {
                return retryTemplate.execute(new NamedRetryable<>(name, action));
            } catch (RetryException e) {
                throw e.getCause();
            }
        }

        @Override
        public R fallback(Function<Throwable, R> fallback) {
            try {
                return retryTemplate.execute(new NamedRetryable<>(name, action));
            } catch (RetryException e) {
                return fallback.apply(e.getCause());
            }
        }
    }

    private class SpringRunSpec implements RunSpec {
        private final String name;
        private final Runnable action;

        SpringRunSpec(String name, Runnable action) {
            this.name = name;
            this.action = action;
        }

        @Override
        public void execute() throws Throwable {
            try {
                retryTemplate.execute(new NamedRetryable<>(name, () -> {
                    action.run();
                    return null;
                }));
            } catch (RetryException e) {
                throw e.getCause();
            }
        }

        @Override
        public void fallback(Consumer<Throwable> fallback) {
            try {
                retryTemplate.execute(new NamedRetryable<>(name, () -> {
                    action.run();
                    return null;
                }));
            } catch (RetryException e) {
                fallback.accept(e.getCause());
            }
        }
    }
}
