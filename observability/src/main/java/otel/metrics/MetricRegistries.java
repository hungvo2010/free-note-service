package otel.metrics;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.Meter;
import lombok.Getter;
import otel.metrics.core.impl.OtelAccumulateMetric;
import otel.metrics.core.impl.OtelLatencyMetric;
import otel.metrics.core.impl.OtelPointInTimeMetric;
import otel.metrics.core.impl.OtelUpDownRealtimeMetric;
import otel.metrics.threads.ThreadMetricsSampler;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

public class MetricRegistries {
    private final Meter meter;
    private final Map<MetricsEnum, Object> registry = new EnumMap<>(MetricsEnum.class);

    public MetricRegistries(Meter meter) {
        this.meter = meter;
    }

    public void registerAll() {
        for (MetricsEnum metric : MetricsEnum.values()) {
            switch (metric.getType()) {
                case GAUGE -> registerGauge(metric);
                case COUNTER -> registerCounter(metric);
                case HISTOGRAM -> registerHistogram(metric);
                case UP_DOWN_COUNTER -> registerUpDownCounter(metric);
            }
        }
    }

    private void registerGauge(MetricsEnum metric) {
        var builder = OtelPointInTimeMetric.<Long>builder()
                .meter(meter)
                .title(metric.getTitle())
                .desc(metric.getDescription())
                .unit(metric.getUnit())
                .type(Long.class);
        if (metric.getAttributedCallbacks() != null) {
            builder.attributedCallbacks(metric.getAttributedCallbacks());
        }
        if (metric.getCallback() != null) {
            builder.recordCallback(metric.getCallback());
        }
        var gauge = builder.build().register();
        registry.put(metric, gauge);
    }

    private void registerCounter(MetricsEnum metric) {
        var counter = OtelAccumulateMetric.builder()
                .meter(meter)
                .title(metric.getTitle())
                .desc(metric.getDescription())
                .unit(metric.getUnit())
                .build()
                .register();
        registry.put(metric, counter);
    }

    private void registerHistogram(MetricsEnum metric) {
        var histogram = OtelLatencyMetric.builder()
                .meter(meter)
                .title(metric.getTitle())
                .desc(metric.getDescription())
                .unit(metric.getUnit())
                .build()
                .register();
        registry.put(metric, histogram);
    }

    private void registerUpDownCounter(MetricsEnum metric) {
        var upDownCounter = OtelUpDownRealtimeMetric.<Long>builder()
                .meter(meter)
                .title(metric.getTitle())
                .desc(metric.getDescription())
                .unit(metric.getUnit())
                .build()
                .register();
        registry.put(metric, upDownCounter);
    }

    // Explicit Getters to avoid exposing MetricsEnum
    public OtelUpDownRealtimeMetric<Long> getConcurrentUsersCounter() {
        return (OtelUpDownRealtimeMetric<Long>) registry.get(MetricsEnum.WEBSOCKET_CONCURRENT_USERS);
    }

    public OtelAccumulateMetric getAcceptedHandshakeCounter() {
        return (OtelAccumulateMetric) registry.get(MetricsEnum.WEBSOCKET_ACCEPT_HANDSHAKE);
    }

    public OtelLatencyMetric getLatencyHistogram() {
        return (OtelLatencyMetric) registry.get(MetricsEnum.WEBSOCKET_LATENCY);
    }
    public OtelPointInTimeMetric<Long> getPlatformThreadsGauge() {
        return (OtelPointInTimeMetric<Long>) registry.get(MetricsEnum.JVM_PLATFORM_THREADS);
    }

    public OtelPointInTimeMetric<Long> getVirtualThreadsGauge() {
        return (OtelPointInTimeMetric<Long>) registry.get(MetricsEnum.JVM_VIRTUAL_THREADS);
    }

    public OtelPointInTimeMetric<Long> getDaemonThreadsGauge() {
        return (OtelPointInTimeMetric<Long>) registry.get(MetricsEnum.JVM_DAEMON_THREADS);
    }

    public OtelPointInTimeMetric<Long> getPeakThreadsGauge() {
        return (OtelPointInTimeMetric<Long>) registry.get(MetricsEnum.JVM_PEAK_THREADS);
    }

    public OtelPointInTimeMetric<Long> getSamplerHealthGauge() {
        return (OtelPointInTimeMetric<Long>) registry.get(MetricsEnum.JVM_THREAD_SAMPLER_HEALTH);
    }

    public OtelPointInTimeMetric<Long> getThreadStatesGauge() {
        return (OtelPointInTimeMetric<Long>) registry.get(MetricsEnum.JVM_THREAD_STATES);
    }

    public OtelUpDownRealtimeMetric<Long> getInFlightRequestsCounter() {
        return (OtelUpDownRealtimeMetric<Long>) registry.get(MetricsEnum.WEBSOCKET_IN_FLIGHT_REQUESTS);
    }

    @Getter
    private enum MetricsEnum {
        WEBSOCKET_CONCURRENT_USERS(
                "websocket.concurrent_users",
                "Number of concurrent connected users",
                "1",
                MetricType.UP_DOWN_COUNTER
        ),
        WEBSOCKET_ACCEPT_HANDSHAKE(
                "websocket.accept_handshake.requests",
                "Number of accepted handshake users",
                "1",
                MetricType.COUNTER
        ),
        WEBSOCKET_LATENCY(
                "websocket.latency",
                "Latency of websocket requests",
                "ms",
                MetricType.HISTOGRAM
        ),
        WEBSOCKET_IN_FLIGHT_REQUESTS(
                "websocket.in_flight_requests",
                "Number of messages currently being processed",
                "1",
                MetricType.UP_DOWN_COUNTER
        ),
        JVM_PLATFORM_THREADS(
                "jvm.threads.platform",
                "Number of live platform threads (sampled)",
                "1",
                MetricType.GAUGE,
                () -> ThreadMetricsSampler.getInstance().getPlatformThreads()
        ),
        JVM_VIRTUAL_THREADS(
                "jvm.threads.virtual",
                "Number of live virtual threads (sampled)",
                "1",
                MetricType.GAUGE,
                () -> ThreadMetricsSampler.getInstance().getVirtualThreads()
        ),
        JVM_DAEMON_THREADS(
                "jvm.threads.daemon",
                "Number of live daemon threads (sampled)",
                "1",
                MetricType.GAUGE,
                () -> ThreadMetricsSampler.getInstance().getDaemonThreads()
        ),
        JVM_PEAK_THREADS(
                "jvm.threads.peak",
                "Peak number of live platform threads since JVM start (sampled)",
                "1",
                MetricType.GAUGE,
                () -> ThreadMetricsSampler.getInstance().getPeakThreads()
        ),
        JVM_THREAD_SAMPLER_HEALTH(
                "jvm.threads.sampler.seconds_since_last_sample",
                "Seconds since the last successful thread metrics sampling pass — alerts when sampling is stuck",
                "s",
                MetricType.GAUGE,
                () -> ThreadMetricsSampler.getInstance().getSecondsSinceLastSample()
        ),
        JVM_THREAD_STATES(
                "jvm.threads.state",
                "Number of live platform threads by state (sampled), tagged with the 'state' attribute",
                "1",
                MetricType.GAUGE,
                null,
                threadStateCallbacks()
        );

        private final String title;
        private final String description;
        private final String unit;
        private final MetricType type;
        private Supplier<Long> callback;
        private Map<Attributes, Supplier<Long>> attributedCallbacks;

        MetricsEnum(String title, String description, String unit, MetricType type) {
            this.title = title;
            this.description = description;
            this.unit = unit;
            this.type = type;
        }

        MetricsEnum(String title, String description, String unit, MetricType type, Supplier<Long> callback) {
            this.title = title;
            this.description = description;
            this.unit = unit;
            this.type = type;
            this.callback = callback;
        }

        MetricsEnum(String title, String description, String unit, MetricType type, Supplier<Long> callback,
                    Map<Attributes, Supplier<Long>> attributedCallbacks) {
            this.title = title;
            this.description = description;
            this.unit = unit;
            this.type = type;
            this.callback = callback;
            this.attributedCallbacks = attributedCallbacks;
        }

        /**
         * One time series per {@link Thread.State}, tagged via a lowercase
         * {@code state} attribute (e.g. {@code jvm.threads.state{state="blocked"}}).
         */
        private static Map<Attributes, Supplier<Long>> threadStateCallbacks() {
            AttributeKey<String> stateKey = AttributeKey.stringKey("state");
            Map<Attributes, Supplier<Long>> callbacks = new LinkedHashMap<>();
            for (Thread.State state : Thread.State.values()) {
                Attributes attributes = Attributes.of(stateKey, state.name().toLowerCase(Locale.ROOT));
                callbacks.put(attributes, () -> ThreadMetricsSampler.getInstance().getThreadStateCount(state));
            }
            return callbacks;
        }

        public enum MetricType {
            GAUGE, COUNTER, HISTOGRAM, UP_DOWN_COUNTER
        }
    }
}