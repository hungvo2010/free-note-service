package otel.metrics;

import static otel.SampleGlobalOpenTelemetry.SAMPLE_GLOBAL_TELEMETRY;

public class MetricUtils {
    private static final MetricsCollection metricsCollection = SAMPLE_GLOBAL_TELEMETRY.getMetricsCollection();

    public static void incrementConcurrentUsers() {
        metricsCollection.incrementConcurrentUsers();
    }

    public static void decrementConcurrentUsers() {
        metricsCollection.decrementConcurrentUsers();
    }

    public static void incrementAcceptedHandshakeCount() {
        metricsCollection.getAcceptedHandshakeCount().incrementAndGet();
    }

    public static void incrementAcceptedHandshakeCount(int val) {
        metricsCollection.getAccumulateMetrics().get(0).add(val);
    }
}
