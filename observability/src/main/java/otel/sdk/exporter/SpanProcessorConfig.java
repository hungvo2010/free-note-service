package otel.sdk.exporter;

import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;

import java.time.Duration;

public class SpanProcessorConfig {

    public static SpanProcessor batchSpanProcessor(SpanExporter spanExporter) {

        return BatchSpanProcessor.builder(spanExporter)

                .setMaxQueueSize(50)
                .setMaxExportBatchSize(50)

                .setExporterTimeout(Duration.ofSeconds(30))

                .setScheduleDelay(Duration.ofSeconds(15))

                .build();

    }

    public static SpanProcessor simpleSpanProcessor(SpanExporter spanExporter) {

        return SimpleSpanProcessor.builder(spanExporter).build();

    }

}