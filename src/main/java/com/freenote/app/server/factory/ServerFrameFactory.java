package com.freenote.app.server.factory;

import com.freenote.app.server.frames.CloseFrame;
import com.freenote.app.server.frames.FrameType;
import com.freenote.app.server.frames.PingFrame;
import com.freenote.app.server.frames.PongFrame;
import com.freenote.app.server.frames.base.DataFrame;
import com.freenote.app.server.frames.base.WebSocketFrame;

public class ServerFrameFactory implements FrameFactory {
    @Override
    public WebSocketFrame createTextFrame(String text) {
        return new DataFrame(FrameType.TEXT.getOpCode(), text.getBytes());
    }

    @Override
    public WebSocketFrame createBinaryFrame(byte[] data) {
        return new DataFrame(FrameType.BINARY.getOpCode(), data);
    }

    @Override
    public WebSocketFrame createPingFrame() {
        return new PingFrame();
    }

    @Override
    public WebSocketFrame createPongFrame() {
        return new PongFrame();
    }

    @Override
    public WebSocketFrame createCloseFrame(int code, String reason) {
        return new CloseFrame();
    }

    @Override
    public WebSocketFrame createContinuationFrame(byte[] data) {
        return new DataFrame(FrameType.CONTINUATION.getOpCode(), data);
    }
}
