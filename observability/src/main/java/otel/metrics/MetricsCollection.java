package otel.metrics;

import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.ObservableLongGauge;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

public class MetricsCollection {
    @Getter
    private final AtomicLong concurrentUsers = new AtomicLong(0);
    private final List<ObservableLongGauge> gauges = new ArrayList<>();
    @Getter
    private final AtomicLong acceptedHandshakeCount = new AtomicLong(0);
    private final Meter meter;

    public MetricsCollection(Meter meter) {
        this.meter = meter;
    }

    public void incrementConcurrentUsers() {
        concurrentUsers.incrementAndGet();
    }

    public void decrementConcurrentUsers() {
        concurrentUsers.decrementAndGet();
    }

    public void addMetric(ObservableLongGauge gauge) {
        this.gauges.add(gauge);
    }

    public void initMetrics() {
        var concurrentUsersGauge = buildLongGauge(
                "websocket.concurrent_users",
                "Number of concurrent connected users",
                getConcurrentUsers()::get);
        var acceptedHandshake = buildLongGauge(
                "websocket.accept_handshake.requests",
                "Number of accepted handshake users",
                getAcceptedHandshakeCount()::get);

        addMetric(concurrentUsersGauge);
        addMetric(acceptedHandshake);
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
