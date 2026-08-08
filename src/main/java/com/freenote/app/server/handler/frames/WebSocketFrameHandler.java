package com.freenote.app.server.handler.frames;

import com.freenote.app.server.core.WebSocketConnection;
import com.freenote.app.server.model.http.HttpUpgradeRequest;

import java.io.IOException;
import java.nio.ByteBuffer;

public interface WebSocketFrameHandler {
    void onMessage(WebSocketConnection webSocketConnection, String message) throws IOException;

    void onMessage(WebSocketConnection webSocketConnection, ByteBuffer message);

    void onOpen(WebSocketConnection webSocketConnection, HttpUpgradeRequest handshake);

    void onClose(WebSocketConnection webSocketConnection, int code, String reason, boolean remote);

    void onError(WebSocketConnection webSocketConnection, Exception throwable);

    void onPing(WebSocketConnection webSocketConnection, ByteBuffer payload);

    void onPong(WebSocketConnection webSocketConnection, ByteBuffer payload);

    void onContinue(WebSocketConnection webSocketConnection, ByteBuffer payload);
}
