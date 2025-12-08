package com.freenote.app.server.handler.impl;

import com.freenote.app.server.connections.WebSocketConnection;
import com.freenote.app.server.exceptions.ClientDisconnectException;
import com.freenote.app.server.handler.WebSocketHandler;
import com.freenote.app.server.http.HttpUpgradeRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;

public class NewEchoHandlerImpl implements WebSocketHandler {
    private static final Logger log = LogManager.getLogger(NewEchoHandlerImpl.class);

    @Override
    public void onMessage(WebSocketConnection webSocketConnection, String message) {
        log.info("Writing to output stream with message: {}", message);
        log.info("===========================================================================");
        webSocketConnection.send(message);
    }

    @Override
    public void onMessage(WebSocketConnection webSocketConnection, ByteBuffer message) {

    }

    @Override
    public void onOpen(WebSocketConnection webSocketConnection, HttpUpgradeRequest upgradeRequest) {

    }

    @Override
    public void onClose(WebSocketConnection webSocketConnection, int code, String reason, boolean remote) {
        log.warn("Received CLOSE frame. No further processing.");
        throw new ClientDisconnectException("Client sent CLOSE frame");
    }

    @Override
    public void onError(WebSocketConnection webSocketConnection, Exception throwable) {

    }

    @Override
    public void onPing(WebSocketConnection webSocketConnection, ByteBuffer payload) {

    }

    @Override
    public void onPong(WebSocketConnection webSocketConnection, ByteBuffer payload) {

    }

    @Override
    public void onContinue(WebSocketConnection webSocketConnection, ByteBuffer payload) {

    }
}
