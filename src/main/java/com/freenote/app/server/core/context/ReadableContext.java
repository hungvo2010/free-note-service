package com.freenote.app.server.core.context;

import com.freenote.app.server.model.http.HttpUpgradeRequest;
import com.freenote.app.server.model.ws.NetworkRequestData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import otel.metrics.MetricUtils;

import java.io.IOException;
import java.net.SocketAddress;

@AllArgsConstructor
@Getter
@Builder
public class ReadableContext {
    private final NetworkRequestData networkRequestData;
    private final TracingContext tracingContext;
    @Setter
    private HttpUpgradeRequest httpUpgradeRequest;

    public void closeChannel() throws IOException {
        networkRequestData.close();
        MetricUtils.decrementConcurrentUsers();
    }

    public SocketAddress getRemoteAddress() throws IOException {
        return (SocketAddress) networkRequestData.getRemoteAddress();
    }

    public boolean isHandshakeComplete() {
        return httpUpgradeRequest != null;
    }
}
