package com.freenote.app.server.frames.base;

public class ControlFrame extends WebSocketFrame {
    @Override
    protected void parsePayloadLength(byte[] bytes) {
        masked = false;
        payloadLength = 0;
    }
}
