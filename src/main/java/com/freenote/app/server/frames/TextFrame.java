package com.freenote.app.server.frames;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.ObjectOutput;

public class TextFrame extends BaseFrame {
    private static final Logger log = LogManager.getLogger(TextFrame.class);
    private final FrameType opCode = FrameType.TEXT;
    private byte[] payload = new byte[0];

    public TextFrame() {
    }

    public TextFrame(byte[] payload) {
        this.payload = payload;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        log.info("hello from TextFrame");
        if (payload == null || payload.length == 0) {
            throw new IllegalArgumentException("Payload cannot be null or empty");
        }
        var outputBytes = FrameFactory.createServerFrame(payload, opCode);
        out.write(outputBytes);
    }
}
