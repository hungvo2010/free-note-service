package otel.metrics;

import otel.metrics.core.impl.OtelLatencyMetric;
import otel.metrics.threads.ThreadMetricsSampler;

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

    /**
     * Realtime (sampled every ~1s) number of live platform threads.
     */
    public static long getPlatformThreadCount() {
        return ThreadMetricsSampler.getInstance().getPlatformThreads();
    }

    /**
     * Realtime (sampled every ~1s) number of live virtual threads.
     */
    public static long getVirtualThreadCount() {
        return ThreadMetricsSampler.getInstance().getVirtualThreads();
    }

    /**
     * Realtime (sampled every ~1s) number of live platform threads in the given state.
     */
    public static long getThreadStateCount(Thread.State state) {
        return ThreadMetricsSampler.getInstance().getThreadStateCount(state);
    }

    /**
     * Seconds since the last successful sampling pass — &gt;2× interval means the sampler is stuck.
     */
    public static long getThreadSamplerHealth() {
        return ThreadMetricsSampler.getInstance().getSecondsSinceLastSample();
    }

}