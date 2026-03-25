package otel.sdk;

import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.ServiceAttributes;

public class ResourceConfig {
    public static Resource create() {
        return Resource.getDefault().toBuilder()
                .put(ServiceAttributes.SERVICE_NAME, "free-note-service")
                .build();
    }
}
