package com.freenote.app.server.model.ws;

import com.freenote.app.server.frames.ws.WebSocketFrame;

import java.io.IOException;

public interface NetworkRequestData {
    WebSocketFrame buildRequestFrame();

    WebSocketFrame buildResponseFrame();

    byte[] read() throws IOException;

    int read(byte[] data);

    void write(byte[] data) throws IOException;

    // TODO: thinking about this method, mixed level of abstractions with others
    void write(WebSocketFrame frame);
}
