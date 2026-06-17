package com.freenote.app.server.model.ws;

import com.freenote.app.server.frames.ws.WebSocketFrame;

public class BlockingNetworkRequestData implements NetworkRequestData {
    @Override
    public WebSocketFrame buildRequestFrame() {
        return null;
    }

    @Override
    public WebSocketFrame buildResponseFrame() {
        return null;
    }

    @Override
    public byte[] read() {
        return new byte[0];
    }

    @Override
    public int read(byte[] data) {
        return 0;
    }

    @Override
    public void write(byte[] data) {

    }
}
