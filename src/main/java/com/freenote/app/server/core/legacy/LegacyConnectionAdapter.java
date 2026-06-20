package com.freenote.app.server.core.legacy;

import com.freenote.app.server.core.connection.IncomingConnectionHandler;
import com.freenote.app.server.core.connection.WebSocketSession;
import com.freenote.app.server.core.context.ConnectionContext;
import com.freenote.app.server.exceptions.ConnectionException;

public class LegacyConnectionAdapter implements IncomingConnectionHandler {
    private final LegacySessionBasedConnectionHandler legacyHandler;

    public LegacyConnectionAdapter(LegacySessionBasedConnectionHandler legacyHandler) {
        this.legacyHandler = legacyHandler;
    }

    @Override
    public void handle(ConnectionContext context) throws ConnectionException {
        try {
            WebSocketSession session = convertToSession(context);
            legacyHandler.handle(session);
        } catch (Exception e) {
            throw new ConnectionException("Failed to handle connection", e);
        }
    }

    private WebSocketSession convertToSession(ConnectionContext context) {
        return WebSocketSession.builder()
                .networkRequestData(context.getNetworkRequestData())
                .build();
    }
}
