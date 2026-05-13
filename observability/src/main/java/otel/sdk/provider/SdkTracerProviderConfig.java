package otel.sdk.provider;


import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import otel.sdk.exporter.SamplerConfig;
import otel.sdk.exporter.SpanExporterConfig;
import otel.sdk.exporter.SpanLimitsConfig;
import otel.sdk.exporter.SpanProcessorConfig;

public class SdkTracerProviderConfig {

    public static SdkTracerProvider create(Resource resource) {
        String httpEndpoint = getEnvOrDefault("OTLP_HTTP_ENDPOINT", "http://localhost:4318/v1/traces");
        String grpcEndpoint = getEnvOrDefault("OTLP_GRPC_ENDPOINT", "http://localhost:4317");

        return SdkTracerProvider.builder()

                .setResource(resource)
                .addSpanProcessor(SpanProcessorConfig.simpleSpanProcessor(LoggingSpanExporter.create()))
                .addSpanProcessor(SpanProcessorConfig.simpleSpanProcessor(SpanExporterConfig.otlpHttpSpanExporter(httpEndpoint)))
                .addSpanProcessor(SimpleSpanProcessor.create(SpanExporterConfig.otlpGrpcSpanExporter(grpcEndpoint)))
                .setSampler(SamplerConfig.alwaysOn())
                .setSpanLimits(SpanLimitsConfig::spanLimits)

                .build();

    }

    private static String getEnvOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value != null && !value.isBlank()) ? value : defaultValue;
    }

}
