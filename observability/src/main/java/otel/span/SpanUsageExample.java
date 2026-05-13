package otel.span;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.*;
import io.opentelemetry.semconv.NetworkAttributes;

import java.util.Arrays;

import static io.opentelemetry.context.Context.current;

public class SpanUsageExample {

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
