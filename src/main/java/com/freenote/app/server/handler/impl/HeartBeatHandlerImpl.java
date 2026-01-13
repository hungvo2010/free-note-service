package com.freenote.app.server.handler.impl;


import com.freenote.annotations.WebSocketEndpoint;
import com.freenote.app.server.core.WebSocketConnection;
import com.freenote.app.server.frames.factory.ServerFrameFactory;

import java.nio.ByteBuffer;

@WebSocketEndpoint("/heartbeat")
public class HeartBeatHandlerImpl extends CommonEndpointHandlerImpl {
    private final ServerFrameFactory serverFactory = new ServerFrameFactory();


    @Override
    public void onMessage(WebSocketConnection webSocketConnection, String message) {
        webSocketConnection.setResponseObject(null);
    }

    @Override
    public void onData(WebSocketConnection webSocketConnection, String message) {

    }

    @Override
    public void onPing(WebSocketConnection webSocketConnection, ByteBuffer payload) {
        webSocketConnection.setResponseFrame(serverFactory.createPongFrame());
    }

    @Override
    public void onControl(WebSocketConnection webSocketConnection, ByteBuffer payload) {

    }
}
