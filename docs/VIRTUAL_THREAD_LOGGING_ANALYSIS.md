# Analysis: Virtual Thread Logging Issue in ServerBootstrap

## Problem Description
The `log.info` statement inside the virtual thread in `ServerBootstrap.java` was not appearing in the console or log files, despite the thread being correctly started and joined.

```java
Thread t = Thread.ofVirtual()
        .name("my-worker")
        .unstarted(() -> {
            log.info("Running in virtual thread: {}", Thread.currentThread());
        });
t.start();
t.join();
```

## Root Cause Analysis

### 1. Log4j2 Configuration Hierarchy
The primary cause was likely the **lack of a valid `log4j2.xml` on the main project's classpath**.
- The project has `log4j2.xml` files in the `observability` and `trial` modules, but the root `src/main/resources` was empty.
- Without an explicit configuration in the running module's resources, Log4j2 falls back to its **Default Configuration**, which sets the root logging level to **ERROR**.
- Since the log in the virtual thread was set to `INFO`, it was being filtered out by the default configuration.

### 2. Missing Package-Specific Logger
In the existing configuration files (e.g., in the `observability` module), there were specific loggers for `io.opentelemetry` and `com.trial`, but no explicit logger for `com.freenote`. Even if the config was picked up, if the Root level was higher than INFO, `com.freenote` classes would not log unless explicitly configured.

### 3. Virtual Thread "Pinning" & Synchronized Appenders
Log4j2's `ConsoleAppender` uses `synchronized` blocks.
- In Java 21, when a virtual thread enters a `synchronized` block that performs I/O, it **pins** the carrier thread (the underlying platform thread).
- While pinning usually causes performance degradation rather than total silence, in a resource-constrained environment or during early initialization, it can lead to deadlocks or significant delays in log flushing.

## Resolution Steps

1.  **Ensured Classpath Configuration:** Copied a robust `log4j2.xml` to `src/main/resources` to ensure it is bundled with the application.
2.  **Explicit Logger Definition:** Added a specific logger for the `com.freenote` package in the configuration:
    ```xml
    <Logger name="com.freenote" level="info" additivity="false">
        <AppenderRef ref="Console"/>
        <AppenderRef ref="appLog"/>
    </Logger>
    ```
3.  **Diagnostic Elevation:** Temporarily changed the log level to `ERROR` and added `Thread.currentThread().isVirtual()` to the message in `ServerBootstrap.java` to bypass potential level filtering and confirm the execution context during debugging.

## Conclusion
The issue was a combination of missing configuration and level mismatch. By centralizing the `log4j2.xml` and explicitly defining the package logger, the virtual thread's output is now properly captured.
