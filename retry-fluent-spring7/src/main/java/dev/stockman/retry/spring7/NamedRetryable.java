package dev.stockman.retry.spring7;

import org.jspecify.annotations.NullMarked;
import org.springframework.core.retry.Retryable;

import java.util.function.Supplier;

class NamedRetryable<R> implements Retryable<R> {

    private final String name;
    private final Supplier<R> retryable;

    NamedRetryable(String name, Supplier<R> retryable) {
        this.name = name;
        this.retryable = retryable;
    }

    @Override
    public R execute() {
        return retryable.get();
    }

    @Override
    public @NullMarked String getName() {
        return name;
    }
}
