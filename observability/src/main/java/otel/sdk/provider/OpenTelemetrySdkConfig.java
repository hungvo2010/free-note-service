package otel.sdk.provider;

import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import otel.sdk.context.ContextPropagatorsConfig;
import otel.sdk.resources.ResourceConfig;

public class OpenTelemetrySdkConfig {

    public static OpenTelemetrySdk create() {

        Resource resource = ResourceConfig.create();

        return OpenTelemetrySdk.builder()

                .setTracerProvider(SdkTracerProviderConfig.create(resource))

                .setMeterProvider(SdkMeterProviderConfig.create(resource))

        //                .setLoggerProvider(SdkLoggerProviderConfig.create(resource))

                .setPropagators(ContextPropagatorsConfig.create())

                .build();

    }

}