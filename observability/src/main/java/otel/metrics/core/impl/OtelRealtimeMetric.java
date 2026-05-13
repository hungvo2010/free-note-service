package otel.metrics.core.impl;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongUpDownCounter;
import io.opentelemetry.api.metrics.Meter;
import lombok.Builder;
import otel.metrics.core.RealtimeMetric;

import java.util.concurrent.atomic.AtomicLong;

@Builder
public class OtelRealtimeMetric<T extends Number> implements RealtimeMetric<T> {
    private String title;
    @Builder.Default
    private String desc = "default.otel.realtime.metric";
    private Meter meter;
    @Builder.Default
    private String unit = "1";
    private LongUpDownCounter upDownCounter;
    private final AtomicLong internalCounter = new AtomicLong(0);

    public OtelRealtimeMetric<T> register() {
        this.upDownCounter = meter.upDownCounterBuilder(title)
                .setDescription(desc)
                .setUnit(unit)
                .build();
        return this;
    }

    @Override
    public void increment(T val) {
        long value = val.longValue();
        this.upDownCounter.add(value);
        this.internalCounter.addAndGet(value);
    }

    @Override
    public void decrement(T val) {
        long value = val.longValue();
        this.upDownCounter.add(-value);
        this.internalCounter.addAndGet(-value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public T get() {
        return (T) Long.valueOf(internalCounter.get());
    }

    @Override
    public void set(T val) {
        long target = val.longValue();
        long current = internalCounter.get();
        long diff = target - current;
        this.upDownCounter.add(diff);
        this.internalCounter.set(target);
    }

    public void increment(T val, Attributes attributes) {
        long value = val.longValue();
        this.upDownCounter.add(value, attributes);
        this.internalCounter.addAndGet(value);
    }

    public void decrement(T val, Attributes attributes) {
        long value = val.longValue();
        this.upDownCounter.add(-value, attributes);
        this.internalCounter.addAndGet(-value);
    }
}
