package com.freenote.app.server.frames.base;

import java.io.IOException;
import java.io.ObjectOutput;

public class ControlFrame extends WebSocketFrame {
    public ControlFrame() {
    }

    public ControlFrame(short opCode) {
        super(opCode);
    }

    @Override
    protected void parsePayloadLength(byte[] bytes) {
        isMasked = false;
        payloadLength = 0;
    }

    @Override
    public void parseMaskingKey(byte[] bytes) {
        // Control frames do not use masking keys, so this method does nothing.
    }

    @Override
    public void writeFrameMaskHeader(ObjectOutput out) throws IOException {
        var maskByte = isMasked ? (byte) 0x80 : (byte) 0x00;
        var secondByte = (byte) (maskByte | (byte) payloadLength);
        out.writeByte(secondByte);
    }

    @Override
    public void writePayloadLength(ObjectOutput out) {
        // note: control frames do not have a payload length
    }

    @Override
    public void writePayload(ObjectOutput out) {
        // note: control frames do not have a payload
    }

    @Override
    protected void parsePayload(byte[] bytes) {
        // Control frames do not have a payload, so this method does nothing.
    }
}
