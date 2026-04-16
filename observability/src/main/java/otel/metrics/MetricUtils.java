package otel.metrics;

import otel.metrics.core.impl.OtelLatencyMetric;

import static otel.SampleGlobalOpenTelemetry.getSampleGlobalTelemetry;

public class MetricUtils {
    private static final MetricFactory factory = getSampleGlobalTelemetry().getMetricFactory();

    public static void incrementConcurrentUsers() {
        factory.getConcurrentUsersCounter().increment(1L);
    }

    public static void decrementConcurrentUsers() {
        factory.getConcurrentUsersCounter().decrement(1L);
    }

    public static void incrementAcceptedHandshakeCount(int val) {
        factory.getAcceptedHandshakeCounter().add(val);
    }

    public static void incrementInFlightRequests() {
        factory.getInFlightRequestsCounter().increment(1L);
    }

    public static void decrementInFlightRequests() {
        factory.getInFlightRequestsCounter().decrement(1L);
    }

    public static OtelLatencyMetric getLatencyMetric() {
        return factory.getLatencyHistogram();
    }
}