package com.freenote.app.server.frames;

public class TextFrame extends BaseFrame {

    public TextFrame() {
    }

    public TextFrame(byte[] payload) {
        super(FrameType.TEXT.getHexValue(), payload);
    }
}
