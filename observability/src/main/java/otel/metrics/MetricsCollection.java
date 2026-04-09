package otel.metrics;

import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.ObservableLongGauge;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

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
        var concurrentUsersGauge = meter.gaugeBuilder("websocket.concurrent_users")
                .setDescription("Number of concurrent connected users")
                .setUnit("1")
                .ofLongs()
                .buildWithCallback(measurement -> measurement.record(getConcurrentUsers().get()));
        var acceptedHandshake = meter.gaugeBuilder("websocket.accept_handshake.requests")
                .setDescription("Number of accepted handshake users")
                .setUnit("1")
                .ofLongs()
                .buildWithCallback(measurement -> measurement.record(getAcceptedHandshakeCount().get()));

        addMetric(concurrentUsersGauge);
        addMetric(acceptedHandshake);
    }
}
