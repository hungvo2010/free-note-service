package com.freenote.app.server.handler.impl;

import com.freenote.app.server.exceptions.ClientDisconnectException;
import com.freenote.app.server.handler.WebSocketHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class NewEchoHandlerImpl implements WebSocketHandler {
    private static final Logger log = LogManager.getLogger(NewEchoHandlerImpl.class);

    @Override
    public void onClose() {
        log.info("Connection closed");
        throw new ClientDisconnectException("Client disconnected");
    }

    @Override
    public void onPing(byte[] payload) {

    }

    @Override
    public void onPong(byte[] payload) {

    }

    @Override
    public void onContinue(byte[] data) {

    }

    @Override
    public void onText(String message) {

    }

    @Override
    public void onBinary(byte[] data) {

    }
}
