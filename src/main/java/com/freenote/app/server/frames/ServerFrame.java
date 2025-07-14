package com.freenote.app.server.frames;

import com.freenote.app.server.frames.base.WebSocketFrame;

public class ServerFrame extends WebSocketFrame {
    public static ServerFrame emptyFrame() {
        return new ServerFrame();
    }

    @Override
    protected void parsePayloadLength(byte[] bytes) {

    }
}
