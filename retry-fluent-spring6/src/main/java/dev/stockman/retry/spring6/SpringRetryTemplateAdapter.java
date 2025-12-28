package dev.stockman.retry.spring6;

import dev.stockman.retry.Retry;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.support.RetryTemplate;

import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class SpringRetryTemplateAdapter implements Retry {

    public static final String CONTEXT_NAME_KEY = "context.name";
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
            return retryTemplate.execute(createCallback());
        }

        @Override
        public R fallback(Function<Throwable, R> fallback) {
            try {
                return retryTemplate.execute(createCallback());
            } catch (Throwable e) {
                return fallback.apply(e);
            }
        }

        private RetryCallback<R, Throwable> createCallback() {
            return context -> {
                context.setAttribute(CONTEXT_NAME_KEY, name);
                return action.get();
            };
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
            retryTemplate.execute(createCallback());
        }

        @Override
        public void fallback(Consumer<Throwable> fallback) {
            try {
                retryTemplate.execute(createCallback());
            } catch (Throwable e) {
                fallback.accept(e);
            }
        }

        private RetryCallback<Void, Throwable> createCallback() {
            return context -> {
                context.setAttribute(CONTEXT_NAME_KEY, name);
                action.run();
                return null;
            };
        }
    }
}
