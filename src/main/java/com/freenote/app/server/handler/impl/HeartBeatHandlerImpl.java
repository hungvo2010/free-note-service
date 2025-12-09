package com.freenote.app.server.handler.impl;


import com.freenote.annotations.URIHandlerImplementation;
import com.freenote.app.server.connections.WebSocketConnection;
import com.freenote.app.server.frames.factory.ServerFrameFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;

@URIHandlerImplementation("/heartbeat")
public class HeartBeatHandlerImpl extends CommonHandlerImpl {
    private final Logger log = LogManager.getLogger(HeartBeatHandlerImpl.class);
    private final ServerFrameFactory serverFactory = new ServerFrameFactory();


    @Override
    public void onMessage(WebSocketConnection webSocketConnection, String message) {
        webSocketConnection.setResponse("heartbeat acknowledged");
    }

    @Override
    public void onPing(WebSocketConnection webSocketConnection, ByteBuffer payload) {
        webSocketConnection.setFrame(serverFactory.createPongFrame());
    }
}
