package otel.metrics.core.impl;

import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.ObservableDoubleGauge;
import io.opentelemetry.api.metrics.ObservableLongGauge;
import lombok.Builder;
import otel.metrics.core.PointInTimeMetric;

import java.util.function.Supplier;

public class OtelPointInTimeMetric<T extends Number> implements PointInTimeMetric<T> {
    private String title;
    private String desc = "default.otel.point.in.time.metric";
    private ObservableLongGauge longGauge;
    private ObservableDoubleGauge doubleGauge;
    private Meter meter;
    private String unit = "1";
    private Supplier<T> recordCallback = null;
    private Class<T> type;

    @Builder
    public OtelPointInTimeMetric(String title, String desc, String unit, Class<T> type, Meter meter, Supplier<T> recordCallback) {
        this.title = title;
        this.desc = desc;
        this.unit = unit;
        this.type = type;
        this.meter = meter;
        this.recordCallback = recordCallback;
    }


    @Override
    public void record(Supplier<T> value) {
        this.recordCallback = value;
    }

    @Override
    public T get() {
        return null;
    }

    public OtelPointInTimeMetric<T> register() {
        if (this.type == Long.class) {
            this.longGauge = meter.gaugeBuilder(title)
                    .setDescription(desc)
                    .setUnit(unit)
                    .ofLongs()
                    .buildWithCallback(measurement -> measurement.record((Long) this.recordCallback.get()));

        } else {
            this.doubleGauge = meter.gaugeBuilder(title)
                    .setDescription(desc)
                    .setUnit(unit)
                    .buildWithCallback(measurement -> measurement.record((Double) this.recordCallback.get()));
        }
        return this;
    }
}
