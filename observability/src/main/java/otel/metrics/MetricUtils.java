package otel.metrics;

import otel.metrics.core.impl.OtelLatencyMetric;

import static otel.SampleGlobalOpenTelemetry.getSampleGlobalTelemetry;

public class MetricUtils {
    private static final MetricsCollection metricsCollection = getSampleGlobalTelemetry().getMetricsCollection();

    public static void incrementConcurrentUsers() {
        metricsCollection.incrementConcurrentUsers();
    }

    public static void decrementConcurrentUsers() {
        metricsCollection.decrementConcurrentUsers();
    }

    public static void incrementAcceptedHandshakeCount(int val) {
        metricsCollection.getAccumulateMetrics().getFirst().add(val);
    }

    public static OtelLatencyMetric getLatencyMetric() {
        return metricsCollection.getLatencyMetric();
    }
}
