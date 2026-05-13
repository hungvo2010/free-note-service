package com.freenote.app.server.frames.factory;

import com.freenote.app.server.frames.FrameType;
import com.freenote.app.server.frames.base.ControlFrame;
import com.freenote.app.server.frames.base.DataFrame;
import com.freenote.app.server.messages.ws.WebSocketFrame;

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
        var dataFrame = new DataFrame(FrameType.CONTINUATION.getOpCode(), data);
        dataFrame.setFin(false);
        return dataFrame;
    }

    @Override
    public WebSocketFrame createFrameFromBytes(byte[] frameBytes) {
        var serverFrame = DataFrame.fromRawFrameBytes(frameBytes);
        serverFrame.setMasked(false);
        return serverFrame;
    }

    @Override
    public WebSocketFrame createNonFinalFrame(short opCode, byte[] data) {
        if (opCode == FrameType.CONTINUATION.getOpCode()) {
            throw new IllegalArgumentException("Continuation frames cannot be non-final frames.");
        }
        var frame = new DataFrame(opCode, data);
        frame.setFin(false);
        return frame;
    }
}
