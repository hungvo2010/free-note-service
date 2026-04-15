package otel;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.logs.LoggerProvider;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.TracerProvider;
import lombok.Getter;
import otel.metrics.MetricsCollection;
import otel.sdk.provider.OpenTelemetrySdkConfig;

public class SampleGlobalOpenTelemetry {
    private static final String SAMPLE_SCOPE_NAME = "sample.scope.name";
    @Getter
    private Logger sdkLogger;
    @Getter
    private Meter meter;
    @Getter
    private Tracer tracer;
    @Getter
    private MetricsCollection metricsCollection;
    private final OpenTelemetry openTelemetry;

    public static SampleGlobalOpenTelemetry SAMPLE_GLOBAL_TELEMETRY;

    static {
        GlobalOpenTelemetry.set(OpenTelemetrySdkConfig.create());
        SAMPLE_GLOBAL_TELEMETRY = new SampleGlobalOpenTelemetry().initProviders();
    }

    public SampleGlobalOpenTelemetry() {
        this.openTelemetry =
                GlobalOpenTelemetry.isSet() ? GlobalOpenTelemetry.get() : GlobalOpenTelemetry.getOrNoop();
    }

    public SampleGlobalOpenTelemetry initProviders() {
        TracerProvider tracerProvider = openTelemetry.getTracerProvider();
        MeterProvider meterProvider = openTelemetry.getMeterProvider();
        LoggerProvider loggerProvider = openTelemetry.getLogsBridge();
        tracer = tracerProvider.get(SAMPLE_SCOPE_NAME);
        meter = meterProvider.get(SAMPLE_SCOPE_NAME);
        sdkLogger = loggerProvider.get(SAMPLE_SCOPE_NAME);
        metricsCollection = new MetricsCollection(meter);
        metricsCollection.initMetrics();
        return this;
    }

    public static SampleGlobalOpenTelemetry getSampleGlobalTelemetry() {
        return SAMPLE_GLOBAL_TELEMETRY;
    }
}