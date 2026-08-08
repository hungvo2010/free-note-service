package otel.sdk.provider;


import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import otel.sdk.config.AppProperties;
import otel.sdk.exporter.SamplerConfig;
import otel.sdk.exporter.SpanExporterConfig;
import otel.sdk.exporter.SpanLimitsConfig;
import otel.sdk.exporter.SpanProcessorConfig;

public class SdkTracerProviderConfig {

    public static SdkTracerProvider create(Resource resource) {
        String httpEndpoint = AppProperties.getOrDefault("otlp.http.endpoint", "http://localhost:4318/v1/traces");
        String grpcEndpoint = AppProperties.getOrDefault("otlp.grpc.endpoint", "http://localhost:4317");

        return SdkTracerProvider.builder()

                .setResource(resource)
                .addSpanProcessor(SpanProcessorConfig.simpleSpanProcessor(LoggingSpanExporter.create()))
                .addSpanProcessor(SpanProcessorConfig.simpleSpanProcessor(SpanExporterConfig.otlpHttpSpanExporter(httpEndpoint)))
                .addSpanProcessor(SpanProcessorConfig.batchSpanProcessor(SpanExporterConfig.otlpHttpSpanExporter(httpEndpoint)))
                .addSpanProcessor(SimpleSpanProcessor.create(SpanExporterConfig.otlpGrpcSpanExporter(grpcEndpoint)))
                .setSampler(SamplerConfig.alwaysOn())
                .setSpanLimits(SpanLimitsConfig::spanLimits)

                .build();

    }

}
