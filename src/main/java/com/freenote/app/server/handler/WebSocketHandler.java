package com.freenote.app.server.handler;

import com.freenote.app.server.connections.WebSocketConnection;
import com.freenote.app.server.handler.impl.NewEchoHandlerImpl;
import com.freenote.app.server.http.HttpUpgradeRequest;

import java.nio.ByteBuffer;
import java.util.Map;

public interface WebSocketHandler {
    void onMessage(WebSocketConnection webSocketConnection, String message);

    void onMessage(WebSocketConnection webSocketConnection, ByteBuffer message);

    void onOpen(WebSocketConnection webSocketConnection, HttpUpgradeRequest handshake);

    void onClose(WebSocketConnection webSocketConnection, int code, String reason, boolean remote);

    void onError(WebSocketConnection webSocketConnection, Throwable throwable);

    void onPing(WebSocketConnection webSocketConnection, ByteBuffer payload);

    void onPong(WebSocketConnection webSocketConnection, ByteBuffer payload);

    void onContinue(WebSocketConnection webSocketConnection, ByteBuffer payload);

    public static Map<String, WebSocketHandler> allHandlers = Map.of("/v2/echo", new NewEchoHandlerImpl());
}
