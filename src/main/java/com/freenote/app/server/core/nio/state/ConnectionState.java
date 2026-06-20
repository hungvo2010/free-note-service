package com.freenote.app.server.core.nio.state;

import com.freenote.app.server.core.connection.IncomingConnectionHandler;
import com.freenote.app.server.core.context.ConnectionContext;

import java.io.IOException;

public interface ConnectionState {

    /**
     * Handles an I/O readiness event for a connection in this state.
     *
     * @param handler the connection handler (shared interface for blocking and NIO)
     * @param context the connection context carrying network data and handshake state
     * @return the next state for this connection, or {@code this} to stay in the current state
     */
    ConnectionState handle(IncomingConnectionHandler handler, ConnectionContext context) throws IOException;
}
