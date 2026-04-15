package otel.metrics;

import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.ObservableLongGauge;
import lombok.Getter;
import otel.metrics.core.impl.OtelAccumulateMetric;
import otel.metrics.core.impl.OtelLatencyMetric;
import otel.metrics.core.impl.OtelPointInTimeMetric;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

public class MetricsCollection {
    @Getter
    private final AtomicLong concurrentUsers = new AtomicLong(0);
    private final List<OtelPointInTimeMetric> gauges = new ArrayList<>();
    @Getter
    private final List<OtelAccumulateMetric> accumulateMetrics = new ArrayList<>();
    @Getter
    private final AtomicLong acceptedHandshakeCount = new AtomicLong(0);
    private final Meter meter;
    @Getter
    private OtelLatencyMetric latencyMetric;

    public MetricsCollection(Meter meter) {
        this.meter = meter;
    }

    public void incrementConcurrentUsers() {
        concurrentUsers.incrementAndGet();
    }

    public void decrementConcurrentUsers() {
        concurrentUsers.decrementAndGet();
    }

    public void addMetric(OtelPointInTimeMetric<Long> gauge) {
        this.gauges.add(gauge);
    }

    public void initMetrics() {
//        var concurrentUsersGauge = buildLongGauge(
//                "websocket.concurrent_users",
//                "Number of concurrent connected users",
//                getConcurrentUsers()::get);
        var concurrentUsersGauge = OtelPointInTimeMetric.<Long>builder()
                .meter(meter)
                .title("websocket.concurrent_users")
                .desc("Number of concurrent connected users")
                .type(Long.class)
                .recordCallback(getConcurrentUsers()::get)
                .build()
                .register();
        var acceptedHandshake = OtelAccumulateMetric.builder()
                .meter(meter)
                .title("websocket.accept_handshake.requests")
                .desc("Number of accepted handshake users")
                .build()
                .register();
        latencyMetric = OtelLatencyMetric.builder()
                .meter(meter)
                .title("websocket.latency")
                .desc("Latency of websocket requests")
                .build()
                .register();

//        var acceptedHandshake = buildLongGauge(
//                "websocket.accept_handshake.requests","Number of accepted handshake users"
//                ,
//                getAcceptedHandshakeCount()::get);
//
//        addMetric(concurrentUsersGauge);
        accumulateMetrics.add(acceptedHandshake);

    }

    private ObservableLongGauge buildLongGauge(String metricName, String description, Supplier<Long> longSupplier) {
        return meter.gaugeBuilder(metricName)
                .setDescription(description)
                .setUnit("1")
                .ofLongs()
                .buildWithCallback(
                        measurement -> measurement.record(longSupplier.get())
                );
    }
}
