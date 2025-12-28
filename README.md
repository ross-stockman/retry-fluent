
# Retry Fluent

A technology-agnostic, fluent Java API for retrying operations. 

`Retry Fluent` provides a stable facade that shields your business logic from the breaking changes of underlying retry libraries (like the transition between Spring Retry 1.x and 2.x). It replaces clunky template-based execution with a highly readable, builder-style syntax.

## Why Retry Fluent?

- **Architectural Safety**: Your core business logic depends on a pure Java API, not a specific version of Spring or Resilience4j.
- **Fluent Syntax**: Code that reads like a sentence, making retry logic obvious and maintainable.
- **Zero-Dependency Core**: The `retry-fluent-api` module has no third-party dependencies.
- **Pluggable Adapters**: Move from Spring Boot 3 to Spring Boot 4 (or even away from Spring entirely) just by swapping a Maven dependency.

## Modules

- `retry-fluent-api`: The core interfaces (`Retry`, `RetrySpec`, etc.).
- `retry-fluent-spring6`: Implementation adapter for Spring Retry 1.x (Spring Boot 3.x).
- `retry-fluent-spring7`: Implementation adapter for Spring Retry 2.x (Spring Boot 4.x).

## Installation (Maven)

Add the API to your domain/core module:

```xml
<dependency>
    <groupId>dev.stockman</groupId>
    <artifactId>retry-fluent-api</artifactId>
    <version>1.0</version>
</dependency>
```

Add the appropriate adapter to your infrastructure/application module:

```xml
<dependency>
    <groupId>dev.stockman</groupId>
    <artifactId>retry-fluent-spring7</artifactId>
    <version>1.0</version>
</dependency>
```


## Usage Examples

Inject the `Retry` bean and use the fluent API to wrap any logic.

### 1. Simple Retry with a Return Value
Execute a supplier and throw the last exception if all retries fail.

```java
String result = retry.anonymous()
                     .call(() -> userService.fetchData(userId))
                     .execute();
```


### 2. Named Operation with Fallback
Providing a name improves logging and metrics context. If retries fail, a fallback value is returned.

```java
String user = retry.named("GetUserData")
                   .call(() -> api.getUser(id))
                   .fallback(throwable -> "Guest User");
```


### 3. Void Operations
Works seamlessly with `Runnable` tasks.

```java
retry.named("ProcessOrder")
     .run(() -> orderQueue.send(order))
     .fallback(ex -> log.error("Permanent failure sending order", ex));
```


### 4. Direct Execution (No Fallback)
If you want the exception to propagate out of the retry block after exhaustion:

```java
try {
    retry.named("DatabaseUpdate")
         .run(() -> repository.save(entity))
         .execute();
} catch (Throwable e) {
    // Handle the final failure
}
```


## Creating Your Own Implementation

To support a new retry engine (e.g., Resilience4j), simply implement the `Retry` interface:

```java
public class MyCustomRetryAdapter implements Retry {
    @Override
    public RetrySpec named(String operationName) {
        return new MyCustomRetrySpec(operationName);
    }
    // ...
}
```


## License

This project is licensed under the MIT License.
