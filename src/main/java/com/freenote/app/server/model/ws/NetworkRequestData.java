package com.freenote.app.server.model.ws;

import com.freenote.app.server.frames.ws.WebSocketFrame;

public interface NetworkRequestData {
    WebSocketFrame buildRequestFrame();

    WebSocketFrame buildResponseFrame();

    byte[] read();

    void write(byte[] data);
}
