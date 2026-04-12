package otel.metrics;

import io.opentelemetry.api.metrics.LongUpDownCounter;
import otel.metrics.core.RealtimeMetric;

public class MetricFactory {
    // latency: Histogram
    // concurrent users: UpDownCounter
    // memory usage: Gauge
    // current thread active: Gauge
    // number of requests to 3rd api: Counter

    public RealtimeMetric<Long> buildRealtimeRequestMetrics() {
        return null;
    }

    private LongUpDownCounter buildUpDownCounter() {
        return null;
    }

}
