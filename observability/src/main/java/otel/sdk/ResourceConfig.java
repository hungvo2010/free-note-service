package otel.sdk;

import io.opentelemetry.sdk.resources.Resource;

public class ResourceConfig {
    public static Resource create() {
        return Resource.getDefault().toBuilder().put("SERVICE_NAME", "my-service").build();
    }
}
