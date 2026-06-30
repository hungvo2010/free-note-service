package com.freenote.app.server.core.nio.state;

import com.freenote.app.server.core.context.ConnectionContext;

public interface ConnectionState {

    ConnectionState transition(ConnectionContext context);
}
