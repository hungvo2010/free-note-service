package otel.metrics;

import static otel.GlobalOpenTelemetryManualInstrumentationUsage.sampleTelemetry;

public class MetricUtils {
    private static MetricsCollection metricsCollection = sampleTelemetry.getMetricsCollection();

    public static void incrementConcurrentUsers() {
        metricsCollection.incrementConcurrentUsers();
    }

    public static void decrementConcurrentUsers() {
        metricsCollection.decrementConcurrentUsers();
    }

        public static void incrementAcceptedHandshakeCount() {
            metricsCollection.getAcceptedHandshakeCount().incrementAndGet();
        }
}
