package otel.metrics;

import io.opentelemetry.api.metrics.Meter;
import lombok.Getter;
import otel.metrics.core.impl.OtelAccumulateMetric;
import otel.metrics.core.impl.OtelLatencyMetric;
import otel.metrics.core.impl.OtelPointInTimeMetric;
import otel.metrics.core.impl.OtelRealtimeMetric;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

public class MetricFactory {
    private final Meter meter;
    private final Map<MetricsEnum, Object> registry = new EnumMap<>(MetricsEnum.class);

    public MetricFactory(Meter meter) {
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
        var gauge = OtelPointInTimeMetric.<Long>builder()
                .meter(meter)
                .title(metric.getTitle())
                .desc(metric.getDescription())
                .unit(metric.getUnit())
                .type(Long.class)
                .recordCallback(metric.getCallback())
                .build()
                .register();
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
        var upDownCounter = OtelRealtimeMetric.<Long>builder()
                .meter(meter)
                .title(metric.getTitle())
                .desc(metric.getDescription())
                .unit(metric.getUnit())
                .build()
                .register();
        registry.put(metric, upDownCounter);
    }

    // Explicit Getters to avoid exposing MetricsEnum
    public OtelRealtimeMetric<Long> getConcurrentUsersCounter() {
        return (OtelRealtimeMetric<Long>) registry.get(MetricsEnum.WEBSOCKET_CONCURRENT_USERS);
    }

    public OtelAccumulateMetric getAcceptedHandshakeCounter() {
        return (OtelAccumulateMetric) registry.get(MetricsEnum.WEBSOCKET_ACCEPT_HANDSHAKE);
    }

    public OtelLatencyMetric getLatencyHistogram() {
        return (OtelLatencyMetric) registry.get(MetricsEnum.WEBSOCKET_LATENCY);
    }

    public OtelRealtimeMetric<Long> getInFlightRequestsCounter() {
        return (OtelRealtimeMetric<Long>) registry.get(MetricsEnum.WEBSOCKET_IN_FLIGHT_REQUESTS);
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
        );

        private final String title;
        private final String description;
        private final String unit;
        private final MetricType type;
        private Supplier<Long> callback;

        MetricsEnum(String title, String description, String unit, MetricType type) {
            this.title = title;
            this.description = description;
            this.unit = unit;
            this.type = type;
        }

        public enum MetricType {
            GAUGE, COUNTER, HISTOGRAM, UP_DOWN_COUNTER
        }
    }
}