package com.freenote.app.server.messages;

import com.freenote.app.server.core.connection.WebSocketConnection;
import com.freenote.app.server.handler.endpoint.AbstractEndpointHandler;

public interface IncomingMessage {
    void handle(AbstractEndpointHandler handler, WebSocketConnection connection);
}
