package otel.metrics.core;

import java.util.function.Supplier;

// Work with Gauge
public interface PointInTimeMetric<T extends Number> {
    void record(Supplier<T> value);

    T get();
}
