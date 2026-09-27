package com.freenote.app.server.core.nio.state;

import com.freenote.app.server.core.context.ConnectionContext;
import com.freenote.app.server.model.http.HttpUpgradeRequest;

public interface ConnectionState {

    ConnectionState transition(ConnectionContext context, HttpUpgradeRequest upgradeRequest);
}
