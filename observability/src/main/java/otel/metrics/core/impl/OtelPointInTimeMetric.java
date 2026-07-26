package otel.metrics.core.impl;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.ObservableDoubleGauge;
import io.opentelemetry.api.metrics.ObservableLongGauge;
import lombok.Builder;
import otel.metrics.core.PointInTimeMetric;

import java.util.Map;
import java.util.function.Supplier;
import java.util.logging.Logger;

public class OtelPointInTimeMetric<T extends Number> implements PointInTimeMetric<T> {
    private static final Logger log = Logger.getLogger(OtelPointInTimeMetric.class.getName());

    private String title;
    private String desc = "default.otel.point.in.time.metric";
    private ObservableLongGauge longGauge;
    private ObservableDoubleGauge doubleGauge;
    private Meter meter;
    private String unit = "1";
    private Supplier<T> recordCallback = null;
    /**
     * Optional multi-series callback: when set, each map entry is recorded as
     * its own measurement tagged with the entry's {@link Attributes} (takes
     * precedence over {@link #recordCallback}).
     */
    private Map<Attributes, Supplier<T>> attributedCallbacks = null;
    private Class<T> type;

    @Builder
    public OtelPointInTimeMetric(String title, String desc, String unit, Class<T> type, Meter meter,
                                 Supplier<T> recordCallback, Map<Attributes, Supplier<T>> attributedCallbacks) {
        this.title = title;
        this.desc = desc;
        this.unit = unit;
        this.type = type;
        this.meter = meter;
        this.recordCallback = recordCallback;
        this.attributedCallbacks = attributedCallbacks;
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
        int attributedCount = attributedCallbacks != null ? attributedCallbacks.size() : 0;
        boolean hasSingleCallback = recordCallback != null;
        log.info(() -> String.format(
                "register() called — title=%s  attributedCallbacks=%d  singleCallback=%s  thread=%s",
                title, attributedCount, hasSingleCallback, Thread.currentThread().getName()));

        if (this.type == Long.class) {
            this.longGauge = meter.gaugeBuilder(title)
                    .setDescription(desc)
                    .setUnit(unit)
                    .ofLongs()
                    .buildWithCallback(measurement -> {
                        if (attributedCallbacks != null) {
                            attributedCallbacks.forEach((attributes, supplier) -> {
                                Long val = (Long) supplier.get();
                                log.info(() -> "attr callback — title=" + title + "  attrs=" + attributes + "  val=" + val);
                                measurement.record(val, attributes);
                            });
                        } else if (this.recordCallback != null) {
                            Long val = (Long) this.recordCallback.get();
                            measurement.record(val);
                        }
                    });

        } else {
            this.doubleGauge = meter.gaugeBuilder(title)
                    .setDescription(desc)
                    .setUnit(unit)
                    .buildWithCallback(measurement -> {
                        if (attributedCallbacks != null) {
                            attributedCallbacks.forEach((attributes, supplier) -> {
                                Double val = (Double) supplier.get();
                                measurement.record(val, attributes);
                            });
                        } else if (this.recordCallback != null) {
                            Double val = (Double) this.recordCallback.get();
                            measurement.record(val);
                        }
                    });
        }
        return this;
    }
}
