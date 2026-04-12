package otel.metrics.core.impl;

import io.opentelemetry.api.common.Attributes;
import otel.metrics.core.LatencyMetric;

import java.util.Timer;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class OtelLatencyMetric implements LatencyMetric {
    @Override
    public void record(double duration, TimeUnit unit) {
        
    }

    @Override
    public void record(double duration, TimeUnit unit, Attributes attr) {

    }

    @Override
    public Timer start() {
        return null;
    }

    @Override
    public void stop(Timer ticket) {

    }

    @Override
    public <T> T time(Supplier<T> action) {
        return null;
    }

    @Override
    public void time(Runnable action) {

    }
}
