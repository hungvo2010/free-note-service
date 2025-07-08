package com.freenote.app.server.frames;

import java.util.Random;

public class TextFrame extends BaseFrame {

    public TextFrame() {
        super(FrameType.TEXT.getOpCode(), "".getBytes());
    }

    private TextFrame(byte[] payload, boolean masked) {
        super(FrameType.TEXT.getHexValue(), payload);
        this.masked = masked;
        if (masked) {
            this.maskingKey = new byte[4];
            new Random().nextBytes(this.maskingKey);
        }
    }

    public static TextFrame createClientFrame(byte[] payload) {
        return new TextFrame(payload, true);
    }

    public static TextFrame createServerFrame(byte[] payload) {
        return new TextFrame(payload, false);
    }
}
