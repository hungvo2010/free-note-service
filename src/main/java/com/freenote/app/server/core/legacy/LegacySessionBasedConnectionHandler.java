package com.freenote.app.server.core.legacy;

import com.freenote.app.server.core.model.connection.WebSocketSession;

import java.io.IOException;

public interface LegacySessionBasedConnectionHandler {
    void handle(WebSocketSession session) throws IOException;
}

