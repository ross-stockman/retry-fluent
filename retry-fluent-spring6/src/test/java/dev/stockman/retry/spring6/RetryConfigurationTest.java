package dev.stockman.retry.spring6;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import dev.stockman.retry.Retry;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@DisplayNameGeneration(CamelCaseToSentences.class)
@SpringJUnitConfig(classes = RetryConfiguration.class)
@TestPropertySource(properties = {
        "retry.maxAttempts=3",
        "retry.initialInterval=50",
        "retry.multiplier=2",
        "retry.maxInterval=1000",
        "retry.jitter=10",
        "retry.retryableExceptions=java.lang.RuntimeException",
        "retry.nonRetryableExceptions=java.lang.IllegalArgumentException"
})
public class RetryConfigurationTest {

    @Autowired
    private RetryTemplate retryTemplate;

    private final RetryableService retryableService = Mockito.mock(RetryableService.class);
    private Retry retry;
    private TestLogAppender logAppender;

    @BeforeEach
    void setup() {
        retry = new SpringRetryTemplateAdapter(retryTemplate);

        Logger logger = (Logger) LoggerFactory.getLogger(RetryLoggerListener.class);
        logAppender = new TestLogAppender();
        logAppender.start();
        logger.addAppender(logAppender);
    }

    @Nested
    class RetryableStringOutput {

        @Test
        void testNoErrors() {
            Mockito.when(retryableService.testString()).thenReturn("No errors");

            Assertions.assertAll(
                    () -> Assertions.assertEquals("No errors", retry.anonymous().call(retryableService::testString).execute()),
                    () -> Assertions.assertEquals("No errors", retry.anonymous().call(retryableService::testString).fallback(_ -> "Fallback"))
            );

            Mockito.verify(retryableService, Mockito.times(2)).testString();
        }

        @Test
        void testRetryableException() {
            Mockito.when(retryableService.testString()).thenThrow(new RuntimeException("Test exception"));

            Assertions.assertAll(
                    () -> Assertions.assertThrows(RuntimeException.class, () -> retry.anonymous().call(retryableService::testString).execute()),
                    () -> Assertions.assertEquals("Fallback", retry.anonymous().call(retryableService::testString).fallback(_ -> "Fallback"))
            );

            Mockito.verify(retryableService, Mockito.times(6)).testString();
        }

        @Test
        void testNonRetryableException() {
            Mockito.when(retryableService.testString()).thenThrow(new IllegalArgumentException("Test exception"));

            Assertions.assertAll(
                    () -> Assertions.assertThrows(IllegalArgumentException.class, () -> retry.anonymous().call(retryableService::testString).execute()),
                    () -> Assertions.assertEquals("Fallback", retry.anonymous().call(retryableService::testString).fallback(_ -> "Fallback"))
            );

            Mockito.verify(retryableService, Mockito.times(2)).testString();
        }

        @Test
        void testRetryThenSuccess() {
            Mockito.when(retryableService.testString())
                    .thenThrow(new RuntimeException("Test exception"))
                    .thenReturn("Retry once, then succeed")
                    .thenThrow(new RuntimeException("Test exception"))
                    .thenReturn("Retry once, then succeed");

            Assertions.assertAll(
                    () -> Assertions.assertEquals("Retry once, then succeed", retry.anonymous().call(retryableService::testString).execute()),
                    () -> Assertions.assertEquals("Retry once, then succeed", retry.anonymous().call(retryableService::testString).fallback(_ -> "Fallback"))
            );

            Mockito.verify(retryableService, Mockito.times(4)).testString();
        }

        @Test
        void testRetryTwiceThenSuccess() {
            Mockito.when(retryableService.testString())
                    .thenThrow(new RuntimeException("Test exception"))
                    .thenThrow(new RuntimeException("Test exception"))
                    .thenReturn("Retry once, then succeed")
                    .thenThrow(new RuntimeException("Test exception"))
                    .thenThrow(new RuntimeException("Test exception"))
                    .thenReturn("Retry once, then succeed");

            Assertions.assertAll(
                    () -> Assertions.assertEquals("Retry once, then succeed", retry.anonymous().call(retryableService::testString).execute()),
                    () -> Assertions.assertEquals("Retry once, then succeed", retry.anonymous().call(retryableService::testString).fallback(_ -> "Fallback"))
            );

            Mockito.verify(retryableService, Mockito.times(6)).testString();
        }

        @Test
        void testRetryThenNonRetryable() {
            Mockito.when(retryableService.testString())
                    .thenThrow(new RuntimeException("Test exception"))
                    .thenThrow(new IllegalArgumentException("Test exception"))
                    .thenThrow(new RuntimeException("Test exception"))
                    .thenThrow(new IllegalArgumentException("Test exception"));

            Assertions.assertAll(
                    () -> Assertions.assertThrows(IllegalArgumentException.class, () -> retry.anonymous().call(retryableService::testString).execute()),
                    () -> Assertions.assertEquals("Fallback", retry.anonymous().call(retryableService::testString).fallback(_ -> "Fallback"))
            );

            Mockito.verify(retryableService, Mockito.times(4)).testString();
        }

        @Test
        void testRetryTwiceThenNonRetryable() {
            Mockito.when(retryableService.testString())
                    .thenThrow(new RuntimeException("Test exception"))
                    .thenThrow(new RuntimeException("Test exception"))
                    .thenThrow(new IllegalArgumentException("Test exception"))
                    .thenThrow(new RuntimeException("Test exception"))
                    .thenThrow(new RuntimeException("Test exception"))
                    .thenThrow(new IllegalArgumentException("Test exception"));

            Assertions.assertAll(
                    () -> Assertions.assertThrows(IllegalArgumentException.class, () -> retry.anonymous().call(retryableService::testString).execute()),
                    () -> Assertions.assertEquals("Fallback", retry.anonymous().call(retryableService::testString).fallback(_ -> "Fallback"))
            );

            Mockito.verify(retryableService, Mockito.times(6)).testString();
        }
    }

    @Nested
    class RetryableVoidOutput {

        @Test
        void testNoErrors() {
            Mockito.doAnswer(_ -> null).when(retryableService).testVoid();

            Assertions.assertAll(
                    () -> Assertions.assertDoesNotThrow(() -> retry.anonymous().run(retryableService::testVoid).execute()),
                    () -> Assertions.assertDoesNotThrow(() -> retry.anonymous().run(retryableService::testVoid).fallback(_ -> {}))
            );

            Mockito.verify(retryableService, Mockito.times(2)).testVoid();
        }

        @Test
        void testRetryableException() {
            Mockito.doAnswer(_ -> {
                throw new RuntimeException("Test exception");
            }).when(retryableService).testVoid();

            Assertions.assertAll(
                    () -> Assertions.assertThrows(RuntimeException.class, () -> retry.anonymous().run(retryableService::testVoid).execute()),
                    () -> Assertions.assertDoesNotThrow(() -> retry.anonymous().run(retryableService::testVoid).fallback(_ -> {}))
            );

            Mockito.verify(retryableService, Mockito.times(6)).testVoid();
        }

        @Test
        void testNonRetryableException() {
            Mockito.doAnswer(_ -> {
                throw new IllegalArgumentException("Test exception");
            }).when(retryableService).testVoid();

            Assertions.assertAll(
                    () -> Assertions.assertThrows(IllegalArgumentException.class, () -> retry.anonymous().run(retryableService::testVoid).execute()),
                    () -> Assertions.assertDoesNotThrow(() -> retry.anonymous().run(retryableService::testVoid).fallback(_ -> {}))
            );

            Mockito.verify(retryableService, Mockito.times(2)).testVoid();
        }

        @Test
        void testRetryThenSuccess() {
            Mockito.doAnswer(_ -> {
                throw new RuntimeException("Test exception");
            }).doAnswer(_ -> null).doAnswer(_ -> {
                throw new RuntimeException("Test exception");
            }).doAnswer(_ -> null).when(retryableService).testVoid();

            Assertions.assertAll(
                    () -> Assertions.assertDoesNotThrow(() -> retry.anonymous().run(retryableService::testVoid).execute()),
                    () -> Assertions.assertDoesNotThrow(() -> retry.anonymous().run(retryableService::testVoid).fallback(_ -> {}))
            );

            Mockito.verify(retryableService, Mockito.times(4)).testVoid();
        }

        @Test
        void testRetryTwiceThenSuccess() {
            Mockito.doAnswer(_ -> {
                throw new RuntimeException("Test exception");
            }).doAnswer(_ -> {
                throw new RuntimeException("Test exception");
            }).doAnswer(_ -> null).doAnswer(_ -> {
                throw new RuntimeException("Test exception");
            }).doAnswer(_ -> {
                throw new RuntimeException("Test exception");
            }).doAnswer(_ -> null).when(retryableService).testVoid();

            Assertions.assertAll(
                    () -> Assertions.assertDoesNotThrow(() -> retry.anonymous().run(retryableService::testVoid).execute()),
                    () -> Assertions.assertDoesNotThrow(() -> retry.anonymous().run(retryableService::testVoid).fallback(_ -> {}))
            );

            Mockito.verify(retryableService, Mockito.times(6)).testVoid();
        }

        @Test
        void testRetryThenNonRetryable() {
            Mockito.doAnswer(_ -> {
                throw new RuntimeException("Test exception");
            }).doAnswer(_ -> {
                throw new IllegalArgumentException("Test exception");
            }).doAnswer(_ -> {
                throw new RuntimeException("Test exception");
            }).doAnswer(_ -> {
                throw new IllegalArgumentException("Test exception");
            }).when(retryableService).testVoid();

            Assertions.assertAll(
                    () -> Assertions.assertThrows(IllegalArgumentException.class, () -> retry.anonymous().run(retryableService::testVoid).execute()),
                    () -> Assertions.assertDoesNotThrow(() -> retry.anonymous().run(retryableService::testVoid).fallback(_ -> {}))
            );

            Mockito.verify(retryableService, Mockito.times(4)).testVoid();
        }

        @Test
        void testRetryTwiceThenNonRetryable() {
            Mockito.doAnswer(_ -> {
                throw new RuntimeException("Test exception");
            }).doAnswer(_ -> {
                throw new RuntimeException("Test exception");
            }).doAnswer(_ -> {
                throw new IllegalArgumentException("Test exception");
            }).doAnswer(_ -> {
                throw new RuntimeException("Test exception");
            }).doAnswer(_ -> {
                throw new RuntimeException("Test exception");
            }).doAnswer(_ -> {
                throw new IllegalArgumentException("Test exception");
            }).when(retryableService).testVoid();

            Assertions.assertAll(
                    () -> Assertions.assertThrows(IllegalArgumentException.class, () -> retry.anonymous().run(retryableService::testVoid).execute()),
                    () -> Assertions.assertDoesNotThrow(() -> retry.anonymous().run(retryableService::testVoid).fallback(_ -> {}))
            );

            Mockito.verify(retryableService, Mockito.times(6)).testVoid();

        }

    }

    @Nested
    class RetryListener {

        @Test
        void testNoErrors() {
            Mockito.doAnswer(_ -> null).when(retryableService).testVoid();

            retry.named("testNoErrors").run(retryableService::testVoid).fallback(_ -> {});

            Mockito.verify(retryableService, Mockito.times(1)).testVoid();

            var events = logAppender.getEvents().stream().map(event -> Event.toEvent(event.getFormattedMessage())).toList();
            events.forEach(event -> Assertions.assertEquals("testNoErrors", event.retryName()));
            events.forEach(event -> Assertions.assertEquals(3, event.maxAttempts()));
            Assertions.assertEquals(1, events.size());
            Assertions.assertEquals(EventType.RETRY_SUCCEEDED, events.getFirst().type());
            Assertions.assertEquals(1, events.getFirst().attempt());
        }

        @Test
        void testRetryableException() {
            Mockito.doAnswer(_ -> {
                throw new RuntimeException("Test exception");
            }).when(retryableService).testVoid();

            retry.named("testRetryableException").run(retryableService::testVoid).fallback(_ -> {});

            Mockito.verify(retryableService, Mockito.times(3)).testVoid();

            var events = logAppender.getEvents().stream().map(event -> Event.toEvent(event.getFormattedMessage())).toList();
            events.forEach(event -> Assertions.assertEquals("testRetryableException", event.retryName()));
            events.forEach(event -> Assertions.assertEquals(3, event.maxAttempts()));
            Assertions.assertEquals(4, events.size());
            Assertions.assertEquals(EventType.RETRY_FAILED, events.get(0).type());
            Assertions.assertEquals(EventType.RETRY_FAILED, events.get(1).type());
            Assertions.assertEquals(EventType.RETRY_FAILED, events.get(2).type());
            Assertions.assertEquals(EventType.POLICY_EXHAUSTED, events.get(3).type());
            Assertions.assertEquals(1, events.get(0).attempt());
            Assertions.assertEquals(2, events.get(1).attempt());
            Assertions.assertEquals(3, events.get(2).attempt());
            Assertions.assertEquals(3, events.get(3).attempt());
        }

        @Test
        void testNonRetryableException() {
            Mockito.doAnswer(_ -> {
                throw new IllegalArgumentException("Test exception");
            }).when(retryableService).testVoid();

            retry.named("testNonRetryableException").run(retryableService::testVoid).fallback(_ -> {});

            Mockito.verify(retryableService, Mockito.times(1)).testVoid();

            var events = logAppender.getEvents().stream().map(event -> Event.toEvent(event.getFormattedMessage())).toList();
            events.forEach(event -> Assertions.assertEquals("testNonRetryableException", event.retryName()));
            events.forEach(event -> Assertions.assertEquals(3, event.maxAttempts()));
            Assertions.assertEquals(2, events.size());
            Assertions.assertEquals(EventType.RETRY_FAILED, events.get(0).type());
            Assertions.assertEquals(EventType.POLICY_TERMINATED, events.get(1).type());
            Assertions.assertEquals(1, events.get(0).attempt());
            Assertions.assertEquals(1, events.get(1).attempt());
        }

        @Test
        void testRetryThenSuccess() {
            Mockito.doAnswer(_ -> {
                throw new RuntimeException("Test exception");
            }).doAnswer(_ -> null).when(retryableService).testVoid();

            retry.named("testRetryThenSuccess").run(retryableService::testVoid).fallback(_ -> {});

            Mockito.verify(retryableService, Mockito.times(2)).testVoid();

            var events = logAppender.getEvents().stream().map(event -> Event.toEvent(event.getFormattedMessage())).toList();
            events.forEach(event -> Assertions.assertEquals("testRetryThenSuccess", event.retryName()));
            events.forEach(event -> Assertions.assertEquals(3, event.maxAttempts()));
            Assertions.assertEquals(2, events.size());
            Assertions.assertEquals(EventType.RETRY_FAILED, events.get(0).type());
            Assertions.assertEquals(EventType.RETRY_SUCCEEDED, events.get(1).type());
            Assertions.assertEquals(1, events.get(0).attempt());
            Assertions.assertEquals(2, events.get(1).attempt());
        }

        @Test
        void testRetryTwiceThenSuccess() {
            Mockito.doAnswer(_ -> {
                throw new RuntimeException("Test exception");
            }).doAnswer(_ -> {
                throw new RuntimeException("Test exception");
            }).doAnswer(_ -> null).when(retryableService).testVoid();

            retry.named("testRetryTwiceThenSuccess").run(retryableService::testVoid).fallback(_ -> {});

            Mockito.verify(retryableService, Mockito.times(3)).testVoid();

            var events = logAppender.getEvents().stream().map(event -> Event.toEvent(event.getFormattedMessage())).toList();
            events.forEach(event -> Assertions.assertEquals("testRetryTwiceThenSuccess", event.retryName()));
            events.forEach(event -> Assertions.assertEquals(3, event.maxAttempts()));
            Assertions.assertEquals(3, events.size());
            Assertions.assertEquals(EventType.RETRY_FAILED, events.get(0).type());
            Assertions.assertEquals(EventType.RETRY_FAILED, events.get(1).type());
            Assertions.assertEquals(EventType.RETRY_SUCCEEDED, events.get(2).type());
            Assertions.assertEquals(1, events.get(0).attempt());
            Assertions.assertEquals(2, events.get(1).attempt());
            Assertions.assertEquals(3, events.get(2).attempt());
        }

        @Test
        void testRetryThenNonRetryable() {
            Mockito.doAnswer(_ -> {
                throw new RuntimeException("Test exception");
            }).doAnswer(_ -> {
                throw new IllegalArgumentException("Test exception");
            }).when(retryableService).testVoid();

            retry.named("testRetryThenNonRetryable").run(retryableService::testVoid).fallback(_ -> {});

            Mockito.verify(retryableService, Mockito.times(2)).testVoid();

            var events = logAppender.getEvents().stream().map(event -> Event.toEvent(event.getFormattedMessage())).toList();
            events.forEach(event -> Assertions.assertEquals("testRetryThenNonRetryable", event.retryName()));
            events.forEach(event -> Assertions.assertEquals(3, event.maxAttempts()));
            Assertions.assertEquals(3, events.size());
            Assertions.assertEquals(EventType.RETRY_FAILED, events.get(0).type());
            Assertions.assertEquals(EventType.RETRY_FAILED, events.get(1).type());
            Assertions.assertEquals(EventType.POLICY_TERMINATED, events.get(2).type());
            Assertions.assertEquals(1, events.get(0).attempt());
            Assertions.assertEquals(2, events.get(1).attempt());
            Assertions.assertEquals(2, events.get(2).attempt());
        }

        @Test
        void testRetryTwiceThenNonRetryable() {
            Mockito.doAnswer(_ -> {
                throw new RuntimeException("Test exception");
            }).doAnswer(_ -> {
                throw new RuntimeException("Test exception");
            }).doAnswer(_ -> {
                throw new IllegalArgumentException("Test exception");
            }).when(retryableService).testVoid();

            retry.named("testRetryTwiceThenNonRetryable").run(retryableService::testVoid).fallback(_ -> {});

            Mockito.verify(retryableService, Mockito.times(3)).testVoid();

            var events = logAppender.getEvents().stream().map(event -> Event.toEvent(event.getFormattedMessage())).toList();
            events.forEach(event -> Assertions.assertEquals("testRetryTwiceThenNonRetryable", event.retryName()));
            events.forEach(event -> Assertions.assertEquals(3, event.maxAttempts()));
            Assertions.assertEquals(4, events.size());
            Assertions.assertEquals(EventType.RETRY_FAILED, events.get(0).type());
            Assertions.assertEquals(EventType.RETRY_FAILED, events.get(1).type());
            Assertions.assertEquals(EventType.RETRY_FAILED, events.get(2).type());
            Assertions.assertEquals(EventType.POLICY_TERMINATED, events.get(3).type());
            Assertions.assertEquals(1, events.get(0).attempt());
            Assertions.assertEquals(2, events.get(1).attempt());
            Assertions.assertEquals(3, events.get(2).attempt());
            Assertions.assertEquals(3, events.get(3).attempt());

        }

    }

    private abstract static class RetryableService {
        public abstract String testString();
        public abstract void testVoid();
    }

    // Custom appender to capture log events
    private static class TestLogAppender extends AppenderBase<ILoggingEvent> {

        private final List<ILoggingEvent> events = new ArrayList<>();

        @Override
        protected void append(ILoggingEvent eventObject) {
            events.add(eventObject);
        }

        public List<ILoggingEvent> getEvents() {
            return events;
        }
    }

    enum EventType {RETRY_FAILED, RETRY_SUCCEEDED, POLICY_EXHAUSTED, POLICY_TERMINATED, OTHER}
    record Event(String message, EventType type, Integer attempt, Integer maxAttempts, Class<?> exceptionClass, String retryName){

        static Pattern attemptPattern = Pattern.compile("(\\d+)/(\\d+)");
        static Pattern exceptionPattern = Pattern.compile("(?:exception:|encountered:)\\s+([a-zA-Z0-9._$]+)");

        public static Event toEvent(String log) {
            String message = log.substring(0, log.indexOf('.') + 1);
            String retryName = log.substring(log.lastIndexOf(' ') + 1);

            Matcher matcher = attemptPattern.matcher(message);
            int currentAttempt = 0;
            int maxAttempts = 0;
            if (matcher.find()) {
                currentAttempt = Integer.parseInt(matcher.group(1));
                maxAttempts = Integer.parseInt(matcher.group(2));
            }
            Matcher exMatcher = exceptionPattern.matcher(log);
            Class<?> exceptionClass = null;
            if (exMatcher.find()) {
                exceptionClass = RetryUtils.convertToThrowableClass(exMatcher.group(1));
            }

            EventType type = EventType.OTHER;
            if (message.startsWith("Retry policy exhausted after")) {
                type = EventType.POLICY_EXHAUSTED;
            } else if (message.startsWith("Retry policy terminated after")) {
                type = EventType.POLICY_TERMINATED;
            } else if (message.endsWith("failed.")) {
                type = EventType.RETRY_FAILED;
            } else if (message.endsWith("succeeded.")) {
                type = EventType.RETRY_SUCCEEDED;
            }

            return new Event(message, type, currentAttempt, maxAttempts, exceptionClass, retryName);
        }
    };
}
