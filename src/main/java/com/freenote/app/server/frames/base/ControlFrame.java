package com.freenote.app.server.frames.base;

import java.io.IOException;
import java.io.ObjectOutput;

public class ControlFrame extends WebSocketFrame {
    @Override
    protected void parsePayloadLength(byte[] bytes) {
        masked = false;
        payloadLength = 0;
    }

    @Override
    public void writeFrameMaskHeader(ObjectOutput out) throws IOException {
        var maskByte = masked ? (byte) 0x80 : (byte) 0x00;
        var secondByte = (byte) (maskByte | (byte) payloadLength);
        out.writeByte(secondByte);
    }

    @Override
    public void writePayloadLength(ObjectOutput out) {
        // note: control frames do not have a payload length
    }

    @Override
    public void writePayload(ObjectOutput out) throws IOException {
        // note: control frames do not have a payload
    }
}
