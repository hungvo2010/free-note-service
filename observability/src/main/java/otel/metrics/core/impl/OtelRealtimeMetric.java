package otel.metrics.core.impl;

import otel.metrics.core.RealtimeMetric;

public class OtelRealtimeMetric<T extends Number> implements RealtimeMetric<T> {
    @Override
    public void increment(T val) {

    }

    @Override
    public void decrement(T val) {

    }

    @Override
    public T get() {
        return null;
    }

    @Override
    public void set(T val) {

    }
}
