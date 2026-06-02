package otel.metrics;

import otel.metrics.core.impl.OtelLatencyMetric;

import static otel.SampleGlobalOpenTelemetry.getSampleGlobalTelemetry;

public class MetricUtils {
    private static final MetricRegistries registries = getSampleGlobalTelemetry().getMetricRegistries();

    public static void incrementConcurrentUsers() {
        registries.getConcurrentUsersCounter().increment(1L);
    }

    public static void decrementConcurrentUsers() {
        registries.getConcurrentUsersCounter().decrement(1L);
    }

    public static void incrementAcceptedHandshakeCount(int val) {
        registries.getAcceptedHandshakeCounter().add(val);
    }

    public static void incrementInFlightRequests() {
        registries.getInFlightRequestsCounter().increment(1L);
    }

    public static void decrementInFlightRequests() {
        registries.getInFlightRequestsCounter().decrement(1L);
    }

    public static OtelLatencyMetric getLatencyMetric() {
        return registries.getLatencyHistogram();
    }
}