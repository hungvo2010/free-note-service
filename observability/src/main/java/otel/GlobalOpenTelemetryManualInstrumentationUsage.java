package otel;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.logs.LoggerProvider;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.api.trace.*;
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

    static {
        GlobalOpenTelemetry.set(OpenTelemetrySdkConfig.create());
    }

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

    public Span spanUsage(Tracer tracer, String spanName) {

        // Get a span builder by providing the span name

        Span span =

                tracer

                        .spanBuilder(spanName)

                        // Set span kind

                        .setSpanKind(SpanKind.INTERNAL)

                        // Set attributes

                        .setAttribute(AttributeKey.stringKey("com.acme.string-key"), "value")

                        .setAttribute(AttributeKey.booleanKey("com.acme.bool-key"), true)

                        .setAttribute(AttributeKey.longKey("com.acme.long-key"), 1L)

                        .setAttribute(AttributeKey.doubleKey("com.acme.double-key"), 1.1)

                        .setAttribute(

                                AttributeKey.stringArrayKey("com.acme.string-array-key"),

                                Arrays.asList("value1", "value2"))

                        .setAttribute(

                                AttributeKey.booleanArrayKey("come.acme.bool-array-key"),

                                Arrays.asList(true, false))

                        .setAttribute(

                                AttributeKey.longArrayKey("come.acme.long-array-key"), Arrays.asList(1L, 2L))

                        .setAttribute(

                                AttributeKey.doubleArrayKey("come.acme.double-array-key"), Arrays.asList(1.1, 2.2))

                        // Optionally omit initializing AttributeKey

                        .setAttribute("com.acme.string-key", "value")

                        .setAttribute("com.acme.bool-key", true)

                        .setAttribute("come.acme.long-key", 1L)

                        .setAttribute("come.acme.double-key", 1.1)

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

            span.updateName("new span name");

            // Add additional attributes not available when starting

            span.setAttribute("com.acme.string-key2", "value");

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