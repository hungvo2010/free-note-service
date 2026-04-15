package otel.metrics.core.impl;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.Meter;
import lombok.Builder;
import otel.metrics.core.LatencyMetric;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Builder
public class OtelLatencyMetric implements LatencyMetric {
    private String title;
    @Builder.Default
    private String desc = "default.otel.latency.metric";
    private DoubleHistogram doubleHistogram;
    private Meter meter;
    @Builder.Default
    private String unit = "s";

    private static final List<Double> DURATION_SECONDS_BUCKETS = List.of(
            0.0, 0.005, 0.01, 0.025, 0.05, 0.075,
            0.1, 0.25, 0.5, 0.75, 1.0, 2.5, 5.0, 7.5, 10.0
    );

    @Override
    public void record(double duration, TimeUnit unit) {
        this.doubleHistogram.record(duration);
    }

    @Override
    public void record(double duration, TimeUnit unit, Attributes attr) {
        this.doubleHistogram.record(duration, attr);
    }

    @Override
    public long start() {
        return System.currentTimeMillis();
    }

    @Override
    public void stop(long startTimer) {
        var endTimer = System.currentTimeMillis();
        record(endTimer - startTimer, TimeUnit.MILLISECONDS);
    }

    @Override
    public <T> T time(Supplier<T> action) {
        var startTimer = System.currentTimeMillis();
        var result = action.get();
        var endTimer = System.currentTimeMillis();
        record(endTimer - startTimer, TimeUnit.MILLISECONDS);
        return result;
    }

    @Override
    public void time(Runnable action) {
        long start = System.nanoTime();
        try {
            action.run();
        } finally {
            long duration = System.nanoTime() - start;
            double seconds = duration / 1_000_000_000.0;
            record(seconds, TimeUnit.NANOSECONDS);
        }
    }

    public OtelLatencyMetric register() {
        var builder = this.doubleHistogram = meter.histogramBuilder(title)
                .setDescription(desc)
                .setUnit(this.unit)
                .setExplicitBucketBoundariesAdvice(DURATION_SECONDS_BUCKETS)
                .build();
        return this;
    }
}
