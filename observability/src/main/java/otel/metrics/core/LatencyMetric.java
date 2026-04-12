package otel.metrics.core;

import io.opentelemetry.api.common.Attributes;

import java.util.Timer;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

// A Start Point (T_1): A timestamp in the past.
// An End Point (T_2): A timestamp in the present.
// The Result (D): The difference (T_2 - T_1 = {Duration})

public interface LatencyMetric {
    void record(double duration, TimeUnit unit);
    void record(double duration, TimeUnit unit, Attributes attr);
    Timer start();
    void stop(Timer ticket);
    <T> T time(Supplier<T> action);
    void time(Runnable action);

}
