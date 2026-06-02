package com.freenote.app.server.core.connection;

import com.freenote.app.server.core.context.ConnectionContext;
import com.freenote.app.server.exceptions.ConnectionException;

public interface IncomingConnectionHandler {
    void handle(ConnectionContext context) throws ConnectionException;
}
