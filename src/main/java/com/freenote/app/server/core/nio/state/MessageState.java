package com.freenote.app.server.core.nio.state;

import com.freenote.app.server.core.context.ConnectionContext;
import com.freenote.app.server.model.http.HttpUpgradeRequest;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class MessageState implements ConnectionState {
    private final HttpUpgradeRequest request;

    @Override
    public ConnectionState transition(ConnectionContext context) {
        if (context.getNetworkRequestData().isClosed()) {
            return null;
        }
        return this;
    }
}
