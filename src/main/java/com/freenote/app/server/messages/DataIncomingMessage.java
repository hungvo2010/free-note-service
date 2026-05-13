package com.freenote.app.server.messages;

import com.freenote.app.server.core.connection.WebSocketConnection;
import com.freenote.app.server.handler.endpoint.AbstractEndpointHandler;

public class DataIncomingMessage implements IncomingMessage {
    private final String message;

    public DataIncomingMessage(String message) {
        this.message = message;
    }

    @Override
    public void handle(AbstractEndpointHandler handler, WebSocketConnection connection) {
        handler.onData(connection, message);
    }
}
