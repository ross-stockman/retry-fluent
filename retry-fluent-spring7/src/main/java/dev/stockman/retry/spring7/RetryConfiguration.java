package dev.stockman.retry.spring7;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.retry.RetryListener;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.util.backoff.BackOff;
import org.springframework.util.backoff.ExponentialBackOff;

import java.util.List;

@Configuration
public class RetryConfiguration {

    @Bean
    public RetryTemplate retryTemplate(
            RetryPolicy retryPolicy,
            RetryListener retryListener
    ) {
        var retryTemplate = new RetryTemplate();
        retryTemplate.setRetryPolicy(retryPolicy);
        retryTemplate.setRetryListener(retryListener);
        return retryTemplate;
    }

    @Bean
    public ExponentialBackOff backOffPolicy(
            @Value("${retry.initialInterval:100}") int initialInterval,
            @Value("${retry.multiplier:2}") int multiplier,
            @Value("${retry.maxInterval:5000}") int maxInterval,
            @Value("${retry.jitter:10}") int jitter,
            @Value("${retry.maxAttempts:3}") int maxAttempts) {
        var backOffPolicy = new ExponentialBackOff();
        backOffPolicy.setInitialInterval(initialInterval);
        backOffPolicy.setMultiplier(multiplier);
        backOffPolicy.setMaxInterval(maxInterval);
        backOffPolicy.setMaxAttempts(maxAttempts);
        backOffPolicy.setJitter(jitter);
        return backOffPolicy;
    }

    @Bean
    public RetryPolicy retryPolicy(
            BackOff backOffPolicy,
            @Value("${retry.retryableExceptions:}") List<String> retryableExceptions,
            @Value("${retry.nonRetryableExceptions:}") List<String> nonRetryableExceptions
    ) {
        return RetryPolicy.builder()
                .backOff(backOffPolicy)
                .includes(RetryUtils.throwableList(retryableExceptions))
                .excludes(RetryUtils.throwableList(nonRetryableExceptions))
                .build();
    }

    @Bean
    public RetryListener retryListener(ExponentialBackOff backOffPolicy) {
        return new RetryLoggerListener(backOffPolicy);
    }
}
