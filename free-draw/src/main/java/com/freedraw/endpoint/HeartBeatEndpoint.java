package com.freedraw.endpoint;


import com.freenote.annotations.WebSocketEndpoint;
import com.freenote.app.server.endpoints.AbstractEndpointHandler;
import com.freenote.app.server.frames.factory.FrameFactory;
import com.freenote.app.server.model.connection.WebSocketConnection;

import java.nio.ByteBuffer;

@WebSocketEndpoint("/heartbeat")
public class HeartBeatEndpoint extends AbstractEndpointHandler {
    private final FrameFactory.ServerFrameFactory serverFactory = new FrameFactory.ServerFrameFactory();


    @Override
    public void onMessage(WebSocketConnection webSocketConnection, String message) {
        webSocketConnection.setAppResponseData(null);
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
