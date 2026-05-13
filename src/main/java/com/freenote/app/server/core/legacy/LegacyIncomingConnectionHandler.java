package com.freenote.app.server.core.legacy;


import com.freenote.app.server.core.connection.WebSocketSession;

import java.io.IOException;

public interface LegacyIncomingConnectionHandler {
    void handle(WebSocketSession session) throws IOException;
}

