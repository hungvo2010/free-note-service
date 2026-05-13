package com.freenote.app.server.frames.factory;

import com.freenote.app.server.frames.FrameType;
import com.freenote.app.server.frames.base.ControlFrame;
import com.freenote.app.server.frames.base.DataFrame;
import com.freenote.app.server.messages.ws.WebSocketFrame;

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
        return ControlFrame.ping();
    }

    @Override
    public WebSocketFrame createPongFrame() {
        return ControlFrame.pong();
    }

    @Override
    public WebSocketFrame createCloseFrame(int code, String reason) {
        return ControlFrame.close();
    }

    @Override
    public WebSocketFrame createContinuationFrame(byte[] data) {
        var bytes = new byte[4];
        secureRandom.nextBytes(bytes);
        var dataFrame = new DataFrame(FrameType.CONTINUATION.getOpCode(), data, true, bytes);
        dataFrame.setFin(false);
        return dataFrame;
    }

    @Override
    public WebSocketFrame createFrameFromBytes(byte[] frameBytes) {
        return DataFrame.fromRawFrameBytes(frameBytes);
    }

    @Override
    public WebSocketFrame createNonFinalFrame(short opCode, byte[] data) {
        if (opCode == FrameType.CONTINUATION.getOpCode()) {
            throw new IllegalArgumentException("Continuation frames cannot be non-final frames.");
        }
        var maskingKey = new byte[4];
        secureRandom.nextBytes(maskingKey);
        var frame = new DataFrame(opCode, data, true, maskingKey);
        frame.setFin(false);
        return frame;
    }
}
