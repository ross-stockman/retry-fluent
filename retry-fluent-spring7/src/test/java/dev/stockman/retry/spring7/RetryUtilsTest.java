package dev.stockman.retry.spring7;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


@DisplayNameGeneration(CamelCaseToSentences.class)
class RetryUtilsTest {

    /**
     * Test class for the `throwableList` method in the `RetryUtils` class.
     * <p>
     * The `throwableList` method takes a list of exception class names as strings
     * and converts them to throwable class objects
     * <p>
     * This test class ensures that the method works correctly for different scenarios.
     */

    @Test
    void testValidInput() {
        var throwableList = List.of("java.io.IOException", "java.net.SocketTimeoutException");

        var result = RetryUtils.throwableList(throwableList);

        assertEquals(2, result.size());
        assertEquals(IOException.class, result.get(0));
        assertEquals(SocketTimeoutException.class, result.get(1));
    }

    @Test
    void testEmptyList() {
        List<String> throwableList = List.of();

        var result = RetryUtils.throwableList(throwableList);

        assertTrue(result.isEmpty());
    }

    @Test
    void testNull() {
        var result = RetryUtils.throwableList(null);

        assertTrue(result.isEmpty());
    }

    @Test
    void testNonThrowableClass() {
        List<String> throwableList = List.of("java.lang.String");

        Exception exception = assertThrows(
                IllegalArgumentException.class,
                () -> RetryUtils.throwableList(throwableList)
        );

        assertTrue(exception.getMessage().contains("Class java.lang.String is not a subclass of java.lang.Throwable"));
    }
    
    @Test
    void testClassNotFound() {
        List<String> throwableList = List.of("java.lang.NoSuchClass");
        Exception exception = assertThrows(
                IllegalArgumentException.class,
                () -> RetryUtils.throwableList(throwableList)
        );

        assertTrue(exception.getMessage().contains("Class java.lang.NoSuchClass not found"));
    }

}