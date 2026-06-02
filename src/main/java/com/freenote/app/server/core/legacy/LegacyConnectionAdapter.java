package com.freenote.app.server.core.legacy;

import com.freenote.app.server.core.connection.IncomingConnectionHandler;
import com.freenote.app.server.core.connection.WebSocketSession;
import com.freenote.app.server.core.context.ConnectionContext;
import com.freenote.app.server.exceptions.ConnectionException;
import com.freenote.app.server.model.InputWrapper;
import com.freenote.app.server.model.OutputWrapper;

import java.io.IOException;

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

    private WebSocketSession convertToSession(ConnectionContext context) throws IOException {
        return WebSocketSession.builder()
                .socket(context.getSocket())
                .inputWrapper(new InputWrapper(context.getSocket()))
                .outputWrapper(new OutputWrapper(context.getSocket().getOutputStream()))
                .build();
    }
}
