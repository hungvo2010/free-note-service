package com.freenote.app.server.frames;

import com.freenote.app.server.frames.base.DataFrame;
import com.freenote.app.server.frames.base.FrameTypeWithBehavior;

import java.util.Random;

public class TextFrame extends DataFrame {

    public TextFrame() {
        super(FrameType.TEXT.getOpCode(), "".getBytes());
    }

    private TextFrame(byte[] payload, boolean masked, byte[] maskingKey) {
        super(FrameTypeWithBehavior.TEXT.getOpcode(), payload);
        this.isMasked = masked;
        if (masked) {
            this.maskingKey = maskingKey;
        }
    }

    public static TextFrame createClientFrame(byte[] payload) {
        var maskingKey = new byte[4];
        new Random().nextBytes(maskingKey);
        return new TextFrame(payload, true, maskingKey);
    }

    public static TextFrame createServerFrame(byte[] payload) {
        return new TextFrame(payload, false, new byte[0]);
    }
}
