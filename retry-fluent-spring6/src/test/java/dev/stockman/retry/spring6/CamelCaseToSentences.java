package dev.stockman.retry.spring6;

import org.junit.jupiter.api.DisplayNameGenerator;

import java.lang.reflect.Method;
import java.util.List;

public class CamelCaseToSentences extends DisplayNameGenerator.Standard {
    @Override
    public String generateDisplayNameForMethod(List<Class<?>> enclosingInstanceTypes, Class<?> testClass, Method testMethod) {
        String name = testMethod.getName();
        // This regex finds capital letters and puts a space before them
        String sentence = name.replaceAll("([a-z])([A-Z])", "$1 $2");
        // Capitalize the first letter and return
        return Character.toUpperCase(sentence.charAt(0)) + sentence.substring(1);
    }
}
