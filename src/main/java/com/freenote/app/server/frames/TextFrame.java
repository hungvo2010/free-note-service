package com.freenote.app.server.frames;

import java.io.IOException;
import java.io.ObjectOutput;

public class TextFrame extends BaseFrame {
    private byte[] payload = new byte[0];

    public TextFrame() {

    }

    public TextFrame(byte[] payload) {
        super(FrameType.TEXT.getHexValue());
        this.payload = payload;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        if (payload == null || payload.length == 0) {
            throw new IllegalArgumentException("Payload cannot be null or empty");
        }
        var outputBytes = FrameFactory.createServerFrame(payload, FrameType.TEXT);
        out.write(outputBytes);
    }
}
