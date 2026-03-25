package otel;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.logs.LoggerProvider;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.api.trace.*;
import io.opentelemetry.semconv.NetworkAttributes;
import lombok.Getter;
import otel.sdk.OpenTelemetrySdkConfig;

import java.util.Arrays;

import static io.opentelemetry.context.Context.current;

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

    public static GlobalOpenTelemetryManualInstrumentationUsage sampleTelemetry;

    static {
        GlobalOpenTelemetry.set(OpenTelemetrySdkConfig.create());
        sampleTelemetry = new GlobalOpenTelemetryManualInstrumentationUsage();
    }

    public void globalOpenTelemetryUsage() {
    }

    public GlobalOpenTelemetryManualInstrumentationUsage() {
        this.openTelemetry = GlobalOpenTelemetry.isSet() ? GlobalOpenTelemetry.get() : initializeOpenTelemetry();
    }

    public static OpenTelemetry initializeOpenTelemetry() {
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

    public Span spanUsage(Tracer tracer, String spanName) {

        // Get a span builder by providing the span name

        Span span =

                tracer

                        .spanBuilder(spanName)

                        // Set span kind

                        .setSpanKind(SpanKind.INTERNAL)

                        // Set attributes using Semantic Conventions for standard data
                        .setAttribute(NetworkAttributes.NETWORK_TRANSPORT, "tcp")

                        // Set custom attributes using the app.* namespace for business logic
                        .setAttribute(AttributeKey.stringKey("app.sample.string_key"), "value")

                        .setAttribute(AttributeKey.booleanKey("app.sample.enabled"), true)

                        .setAttribute(AttributeKey.longKey("app.sample.retry_count"), 1L)

                        .setAttribute(AttributeKey.doubleKey("app.sample.threshold"), 1.1)

                        .setAttribute(

                                AttributeKey.stringArrayKey("app.sample.tags"),

                                Arrays.asList("value1", "value2"))

                        .setAttribute(

                                AttributeKey.booleanArrayKey("app.sample.flags"),

                                Arrays.asList(true, false))

                        .setAttribute(

                                AttributeKey.longArrayKey("app.sample.indices"), Arrays.asList(1L, 2L))

                        .setAttribute(

                                AttributeKey.doubleArrayKey("app.sample.ranges"), Arrays.asList(1.1, 2.2))

                        // Optionally omit initializing AttributeKey (still follow app.* naming)

                        .setAttribute("app.sample.short_key", "value")

                        .setAttribute("app.sample.is_test", true)

                        // Uncomment to optionally explicitly set the parent span context. If omitted, the

                        // span's parent will be set using Context.current().

                        // .setParent(parentContext)

                        // Uncomment to optionally add links.

                        // .addLink(linkContext, linkAttributes)

                        // Start the span

                        .startSpan();

        // Check if span is recording before computing additional data

        if (span.isRecording()) {

            // Update the span name with information not available when starting
            // Following the lower-case dot-separated naming convention
            span.updateName("sample.updated_operation");

            // Add additional attributes not available when starting

            span.setAttribute("app.sample.late_binding_key", "value");

            // Add additional span links not available when starting

            span.addLink(exampleLinkContext());

            // optionally include attributes on the link

            // Add span events

            span.addEvent("my-event");

            // Record exception, syntactic sugar for a span event with a specific shape

            span.recordException(new RuntimeException("error"));

            // Set the span status

            span.setStatus(StatusCode.OK, "status description");

        }

        // Finally, end the span
        return span;

    }


    private static SpanContext exampleLinkContext() {

        return Span.fromContext(current()).getSpanContext();

    }
}