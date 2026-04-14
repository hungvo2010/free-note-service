package otel.metrics.core.impl;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.context.Context;
import lombok.Builder;

@Builder
public class OtelAccumulateMetric implements LongCounter {
    private String title;
    private String desc = "default.otel.point.in.time.metric";
    private Meter meter;
    private String unit = "1";
    private LongCounter longCounter;


    public OtelAccumulateMetric register() {
        this.longCounter = meter.counterBuilder(title)
                .setDescription(desc)
                .setUnit(unit)
                .build();
        return this;
    }


    @Override
    public void add(long value) {
        this.longCounter.add(value);
    }

    @Override
    public void add(long value, Attributes attributes) {
        this.longCounter.add(value, attributes);
    }

    @Override
    public void add(long value, Attributes attributes, Context context) {
        this.longCounter.add(value, attributes, context);
    }
}
