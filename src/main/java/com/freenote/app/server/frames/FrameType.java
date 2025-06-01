package com.freenote.app.server.frames;

import lombok.Getter;

@Getter
public enum FrameType {
    CONTINUATION((byte) 0x80),
    TEXT((byte) 0x81),
    BINARY((byte) 0x82),
    CLOSE((byte) 0x88),
    PING((byte) 0x89),
    PONG((byte) 0x8A);

    private final byte hexValue;

    FrameType(byte hexValue) {
        this.hexValue = hexValue;
    }
}
