package com.freenote.app.server.frames;

import lombok.experimental.UtilityClass;

@UtilityClass
public class FrameFactory {
    public static byte[] createWebSocketFrame(byte[] payload, FrameType frameType) {
        byte[] frame = new byte[payload.length + 2];
        frame[0] = frameType.getHexValue(); // FIN bit and opcode for text frame
        frame[1] = (byte) payload.length; // Payload length
        System.arraycopy(payload, 0, frame, 2, payload.length);
        return frame;
    }
}