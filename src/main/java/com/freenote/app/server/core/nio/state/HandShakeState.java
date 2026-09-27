package com.freenote.app.server.core.nio.state;

import com.freenote.app.server.core.event.ConnectionEvent;
import com.freenote.app.server.model.http.HttpUpgradeRequest;

public class HandShakeState implements ConnectionState {

    @Override
    public ConnectionState transition(ConnectionEvent event, HttpUpgradeRequest upgradeRequest) {
        return upgradeRequest != null ? new MessageState(upgradeRequest) : this;
    }
}
