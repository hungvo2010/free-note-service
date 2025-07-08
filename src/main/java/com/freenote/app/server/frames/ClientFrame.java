package com.freenote.app.server.frames;

public class ClientFrame extends BaseFrame {
    public ClientFrame() {
    }

    public ClientFrame(byte frameType, byte[] payload) {
        super(frameType, payload);
    }
}
