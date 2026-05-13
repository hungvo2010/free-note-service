package otel.metrics.core;

// Work with UpDownCounter
public interface RealtimeMetric<T extends Number> {
    void increment(T val);

    void decrement(T val);

    T get();

    void set(T val);
}
