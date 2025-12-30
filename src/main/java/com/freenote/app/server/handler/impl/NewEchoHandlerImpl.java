package com.freenote.app.server.handler.impl;

import com.freenote.annotations.WebSocketEndpoint;
import com.freenote.app.server.core.WebSocketConnection;
import com.freenote.app.server.frames.factory.FrameFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;

@WebSocketEndpoint("/echo")
public class NewEchoHandlerImpl extends CommonEndpointHandlerImpl {
    private static final Logger log = LogManager.getLogger(NewEchoHandlerImpl.class);

    @Override
    public void onMessage(WebSocketConnection webSocketConnection, String message) {
        log.info("Writing to output stream with message: {}", message);
        log.info("===========================================================================");
        webSocketConnection.setResponseFrame(FrameFactory.SERVER.createTextFrame(message));
    }

    @Override
    public void onPing(WebSocketConnection webSocketConnection, ByteBuffer payload) {
        webSocketConnection.setResponseFrame(FrameFactory.SERVER.createPongFrame());
    }

    @Override
    public void onData(WebSocketConnection webSocketConnection, String message) {

    }

    @Override
    public void onControl(WebSocketConnection webSocketConnection, ByteBuffer payload) {

    }
}
