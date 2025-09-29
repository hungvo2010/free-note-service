package com.freenote.app.server.frames.factory;

import com.freenote.app.server.frames.base.WebSocketFrame;

public interface FrameFactory {
    WebSocketFrame createTextFrame(String text);

    WebSocketFrame createBinaryFrame(byte[] data);

    WebSocketFrame createPingFrame();

    WebSocketFrame createPongFrame();

    WebSocketFrame createCloseFrame(int code, String reason);

    WebSocketFrame createContinuationFrame(byte[] data);

    WebSocketFrame createFrameFromBytes(byte[] frameBytes);

    WebSocketFrame createNonFinalFrame(short opCode, byte[] data);
}