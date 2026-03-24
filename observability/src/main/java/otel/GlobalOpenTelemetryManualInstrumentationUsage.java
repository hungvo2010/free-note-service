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

public class GlobalOpenTelemetryManualInstrumentationUsage {
    private static final String SCOPE_NAME = "websocket.lifecycle.full";
    private static final String SCOPE_VERSION = "1.0.0";
    private static final String SCOPE_SCHEMA_URL = "https://github.com/opentelemetry/opentelemetry-java/issues";
    @Getter
    private Logger logger;
    @Getter
    private Meter meter;
    @Getter
    private Tracer tracer;

    private OpenTelemetry openTelemetry;

    public void globalOpenTelemetryUsage() {
        openTelemetry = GlobalOpenTelemetry.isSet() ? GlobalOpenTelemetry.get() : initializeOpenTelemetry();
    }

    public OpenTelemetry initializeOpenTelemetry() {
        return GlobalOpenTelemetry.getOrNoop();
    }

    public void providersUsage() {
        TracerProvider tracerProvider = openTelemetry.getTracerProvider();

        MeterProvider meterProvider = openTelemetry.getMeterProvider();

        LoggerProvider loggerProvider = openTelemetry.getLogsBridge();

        tracer = tracerProvider.get(SCOPE_NAME);

        meter = meterProvider.get(SCOPE_NAME);

        logger = loggerProvider.get(SCOPE_NAME);

    }

    private void setStandardOtelCoreClasses(TracerProvider tracerProvider, MeterProvider meterProvider, LoggerProvider loggerProvider) {
        tracer = tracerProvider.tracerBuilder(SCOPE_NAME).setInstrumentationVersion(SCOPE_VERSION).setSchemaUrl(SCOPE_SCHEMA_URL).build();

        meter = meterProvider.meterBuilder(SCOPE_NAME).setInstrumentationVersion(SCOPE_VERSION).setSchemaUrl(SCOPE_SCHEMA_URL).build();

        logger = loggerProvider.loggerBuilder(SCOPE_NAME).setInstrumentationVersion(SCOPE_VERSION).setSchemaUrl(SCOPE_SCHEMA_URL).build();
    }
}