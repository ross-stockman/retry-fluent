package dev.stockman.retry.spring7;

import java.util.ArrayList;
import java.util.List;

final class RetryUtils {
    private RetryUtils() {
    }

    /**
     * Converts a list of exception class names into a list of Throwable class objects.
     */
    static List<Class<? extends Throwable>> throwableList(List<String> exceptionClassNames) {
        List<Class<? extends Throwable>> exceptionClasses = new ArrayList<>();
        if (exceptionClassNames != null) {
            for (String className : exceptionClassNames) {
                exceptionClasses.add(convertToThrowableClass(className));
            }
        }
        return exceptionClasses;
    }

    /**
     * Converts a single string name into a Throwable class object.
     * Throws IllegalArgumentException if the class is not found or not a Throwable.
     */
    static Class<? extends Throwable> convertToThrowableClass(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            if (Throwable.class.isAssignableFrom(clazz)) {
                return clazz.asSubclass(Throwable.class);
            } else {
                throw new IllegalArgumentException(
                        "Class " + className + " is not a subclass of java.lang.Throwable");
            }
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Class " + className + " not found", e);
        }
    }
}
