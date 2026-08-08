package com.freenote.app.server.model.ws;

import com.freenote.app.server.frames.ws.WebSocketFrame;

import java.io.IOException;

public interface NetworkRequestData {
    WebSocketFrame buildRequestFrame();

    WebSocketFrame buildResponseFrame();

    byte[] readFrameBytes() throws IOException;

    int read(byte[] data) throws IOException;

    void write(byte[] data) throws IOException;

    byte[] read() throws IOException;

    void close() throws IOException;

    boolean isClosed();

    Object getRemoteAddress();

    void prepareForRead();
}
