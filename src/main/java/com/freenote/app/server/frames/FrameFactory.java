package com.freenote.app.server.frames;

import lombok.experimental.UtilityClass;

@UtilityClass
public class FrameFactory {
    public static byte[] createServerFrame(byte[] payload, FrameType frameType) {
        byte[] frame = new byte[payload.length + 2];
        frame[0] = frameType.getHexValue();
        frame[1] = (byte) payload.length;
        System.arraycopy(payload, 0, frame, 2, payload.length);
        return frame;
    }

    public static BaseFrame createGeneralFrame(boolean isFinal, short opcode, byte[] payload) {
        return new BaseFrame(isFinal, opcode, payload);
    }
}