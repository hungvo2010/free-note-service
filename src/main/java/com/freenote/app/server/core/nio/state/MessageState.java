package com.freenote.app.server.core.nio.state;

import com.freenote.app.server.core.event.ConnectionEvent;
import com.freenote.app.server.model.http.HttpUpgradeRequest;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class MessageState implements ConnectionState {
    private final HttpUpgradeRequest request;

    @Override
    public ConnectionState transition(ConnectionEvent event, HttpUpgradeRequest upgradeRequest) {
        if (event.getNetworkRequestData().isClosed()) {
            return null;
        }
        return this;
    }
}
