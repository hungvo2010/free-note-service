package com.freenote.app.server.factory;

import com.freenote.app.server.frames.FrameType;
import com.freenote.app.server.frames.base.ControlFrame;
import com.freenote.app.server.frames.base.DataFrame;
import com.freenote.app.server.frames.base.WebSocketFrame;

import java.security.SecureRandom;

public class ClientFrameFactory implements FrameFactory {
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public WebSocketFrame createTextFrame(String text) {
        var bytes = new byte[4];
        secureRandom.nextBytes(bytes);
        return new DataFrame(FrameType.TEXT.getOpCode(), text.getBytes(), true, bytes);
    }

    @Override
    public WebSocketFrame createBinaryFrame(byte[] data) {
        var bytes = new byte[4];
        secureRandom.nextBytes(bytes);
        return new DataFrame(FrameType.BINARY.getOpCode(), data, true, bytes);
    }

    @Override
    public WebSocketFrame createPingFrame() {
        return new ControlFrame(FrameType.PING.getOpCode(), true);
    }

    @Override
    public WebSocketFrame createPongFrame() {
        return new ControlFrame(FrameType.PONG.getOpCode(), true);
    }

    @Override
    public WebSocketFrame createCloseFrame(int code, String reason) {
        return new ControlFrame(FrameType.CLOSE.getOpCode(), true);
    }

    @Override
    public WebSocketFrame createContinuationFrame(byte[] data) {
        var bytes = new byte[4];
        secureRandom.nextBytes(bytes);
        return new DataFrame(FrameType.CONTINUATION.getOpCode(), data, true, bytes);
    }

    @Override
    public WebSocketFrame createFrameFromBytes(byte[] frameBytes) {
        return DataFrame.fromRawFrameBytes(frameBytes);
    }
}
