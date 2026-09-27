package com.freenote.app.server.core.nio.state;

import com.freenote.app.server.core.event.ConnectionEvent;
import com.freenote.app.server.model.http.HttpUpgradeRequest;

public interface ConnectionState {

    ConnectionState transition(ConnectionEvent event, HttpUpgradeRequest upgradeRequest);
}
