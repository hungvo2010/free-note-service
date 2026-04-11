package otel.sdk.provider;

import io.opentelemetry.exporter.prometheus.PrometheusHttpServer;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.exporter.logging.LoggingMetricExporter;

import java.time.Duration;

public class SdkMeterProviderConfig {
    public static SdkMeterProvider create(Resource resource) {
        return SdkMeterProvider.builder()
                .setResource(resource)
                .registerMetricReader(
                        PeriodicMetricReader.builder(LoggingMetricExporter.create())
                                .setInterval(Duration.ofSeconds(5))
                                .build())
                .registerMetricReader(
                        PrometheusHttpServer.builder().setPort(9464).build())
                .build();
    }
}
