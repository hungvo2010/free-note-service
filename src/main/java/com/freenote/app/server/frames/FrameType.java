package com.freenote.app.server.frames;

import lombok.Getter;

@Getter
public enum FrameType {
    CONTINUATION(true, (short) 0, (byte) 0x80),
    TEXT(true, (short) 1, (byte) 0x81),
    BINARY(true, (short) 2, (byte) 0x82),
    CLOSE(true, (short) 8, (byte) 0x88),
    PING(true, (short) 9, (byte) 0x89),
    PONG(true, (short) 10, (byte) 0x8A);

    private final boolean isFinal;
    private final short opCode;
    private final byte hexValue;

    FrameType(boolean isFinal, short opcode, byte hexValue) {
        this.isFinal = isFinal;
        this.opCode = opcode;
        this.hexValue = hexValue;
    }

    public static FrameType fromHexValue(short opcode) {
        var allFrames = FrameType.values();
        for (FrameType frameType : allFrames) {
            if (frameType.getOpCode() == opcode) {
                return frameType;
            }
        }
        return FrameType.TEXT;
    }
}
