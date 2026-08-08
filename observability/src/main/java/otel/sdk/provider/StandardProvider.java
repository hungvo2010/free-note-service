package otel.sdk.provider;

import io.opentelemetry.api.logs.LoggerProvider;
import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.api.trace.TracerProvider;

@Deprecated
public class StandardProvider {
    private static final String SCOPE_NAME = "websocket.lifecycle.full";
    private static final String SCOPE_VERSION = "1.0.0";
    private static final String SCOPE_SCHEMA_URL = "https://github.com/opentelemetry/opentelemetry-java/issues";

    private void setStandardOtelCoreClasses(TracerProvider tracerProvider, MeterProvider meterProvider, LoggerProvider loggerProvider) {
        var tracer = tracerProvider.tracerBuilder(SCOPE_NAME).setInstrumentationVersion(SCOPE_VERSION).setSchemaUrl(SCOPE_SCHEMA_URL).build();
        var meter = meterProvider.meterBuilder(SCOPE_NAME).setInstrumentationVersion(SCOPE_VERSION).setSchemaUrl(SCOPE_SCHEMA_URL).build();
        var logger = loggerProvider.loggerBuilder(SCOPE_NAME).setInstrumentationVersion(SCOPE_VERSION).setSchemaUrl(SCOPE_SCHEMA_URL).build();
    }
}
