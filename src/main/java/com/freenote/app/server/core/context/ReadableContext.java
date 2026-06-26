package com.freenote.app.server.core.context;

import com.freenote.app.server.model.http.HttpUpgradeRequest;
import com.freenote.app.server.model.ws.NetworkRequestData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import otel.sdk.context.TracingContext;

@AllArgsConstructor
@Getter
@Builder
public class ReadableContext {
    private final NetworkRequestData networkRequestData;
    private final TracingContext tracingContext;
    @Setter
    private HttpUpgradeRequest httpUpgradeRequest;

    public boolean isHandshakeComplete() {
        return httpUpgradeRequest != null;
    }
}
