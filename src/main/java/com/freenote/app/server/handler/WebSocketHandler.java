package com.freenote.app.server.handler;

import com.freenote.app.server.connections.WebSocketConnection;
import com.freenote.app.server.http.HttpUpgradeRequest;

import java.io.IOException;
import java.nio.ByteBuffer;

public interface WebSocketHandler {
    void onMessage(WebSocketConnection webSocketConnection, String message) throws IOException;

    void onMessage(WebSocketConnection webSocketConnection, ByteBuffer message);

    void onOpen(WebSocketConnection webSocketConnection, HttpUpgradeRequest handshake);

    void onClose(WebSocketConnection webSocketConnection, int code, String reason, boolean remote);

    void onError(WebSocketConnection webSocketConnection, Exception throwable);

    void onPing(WebSocketConnection webSocketConnection, ByteBuffer payload);

    void onPong(WebSocketConnection webSocketConnection, ByteBuffer payload);

    void onContinue(WebSocketConnection webSocketConnection, ByteBuffer payload);
}
