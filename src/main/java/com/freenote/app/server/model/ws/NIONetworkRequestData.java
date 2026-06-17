package com.freenote.app.server.model.ws;

import com.freenote.app.server.frames.ws.WebSocketFrame;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class NIONetworkRequestData implements NetworkRequestData {

    private final ByteBuffer byteBuffer;
    private final SocketChannel channel;

    public NIONetworkRequestData(SocketChannel channel, ByteBuffer byteBuffer) {
        this.channel = channel;
        this.byteBuffer = byteBuffer;
    }

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
