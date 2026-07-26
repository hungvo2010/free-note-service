package otel;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.logs.LoggerProvider;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.TracerProvider;
import io.opentelemetry.instrumentation.runtimetelemetry.RuntimeTelemetry;
import lombok.Getter;
import otel.metrics.MetricRegistries;
import otel.metrics.threads.ThreadMetricsSampler;

public class SampleGlobalOpenTelemetry {
    private static final String SAMPLE_SCOPE_NAME = "sample.scope.name";
    @Getter
    private Logger sdkLogger;
    @Getter
    private Meter meter;
    @Getter
    private Tracer tracer;
    @Getter
    private MetricRegistries metricRegistries;
    private final OpenTelemetry openTelemetry;

    public static SampleGlobalOpenTelemetry SAMPLE_GLOBAL_TELEMETRY;

    static {
        SAMPLE_GLOBAL_TELEMETRY = new SampleGlobalOpenTelemetry();
    }

    public SampleGlobalOpenTelemetry() {
        this.openTelemetry =
                GlobalOpenTelemetry.isSet() ? GlobalOpenTelemetry.get() : GlobalOpenTelemetry.getOrNoop();
        RuntimeTelemetry runtimeTelemetry =
                RuntimeTelemetry.builder(openTelemetry)
                        .build();
    }

    public static void init() {
        SAMPLE_GLOBAL_TELEMETRY.initProviders();

    }

    public SampleGlobalOpenTelemetry initProviders() {
        TracerProvider tracerProvider = openTelemetry.getTracerProvider();
        MeterProvider meterProvider = openTelemetry.getMeterProvider();
        LoggerProvider loggerProvider = openTelemetry.getLogsBridge();
        tracer = tracerProvider.get(SAMPLE_SCOPE_NAME);
        meter = meterProvider.get(SAMPLE_SCOPE_NAME);
        sdkLogger = loggerProvider.get(SAMPLE_SCOPE_NAME);

        metricRegistries = new MetricRegistries(meter);
        metricRegistries.registerAll();

        // Start the 1s thread sampler eagerly at boot so the JFR stream is live
        // before any virtual thread is created (JFR only sees threads started later).
        ThreadMetricsSampler.getInstance().start();

        return this;
    }

    public static SampleGlobalOpenTelemetry getSampleGlobalTelemetry() {
        return SAMPLE_GLOBAL_TELEMETRY;
    }
}