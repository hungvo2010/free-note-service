package com.freenote.app.server.frames.base;

import com.freenote.app.server.frames.FrameType;

import java.io.IOException;
import java.io.ObjectOutput;

public class ControlFrame extends WebSocketFrame {
    public ControlFrame() {
        throw new UnsupportedOperationException();
    }

    public ControlFrame(short opCode) {
        super(true, opCode, new byte[0]);
    }

    public ControlFrame(byte[] bytes) {
        super(bytes);
    }

    public ControlFrame(short opCode, boolean isMasked) {
        super(opCode, isMasked);
        this.setFin(true);
    }

    public static ControlFrame ping() {
        return new ControlFrame(FrameType.PING.getOpCode());
    }

    public static ControlFrame pong() {
        return new ControlFrame(FrameType.PONG.getOpCode());
    }

    public static ControlFrame close() {
        return new ControlFrame(FrameType.CLOSE.getOpCode());
    }

    @Override
    protected void parsePayloadLength(byte[] bytes) {
        isMasked = (bytes[1] & 0x80) != 0;
        payloadLength = bytes[1] & 0x7F;
    }

    @Override
    public void parseMaskingKey(byte[] bytes) {
        if (isMasked) {
            maskingKey = new byte[4];
            System.arraycopy(bytes, 2, maskingKey, 0, 4);
        }
    }

    @Override
    protected void parsePayload(byte[] bytes) {
        if (payloadLength > 0) {
            int headerOffset = 2 + (isMasked ? 4 : 0);
            payloadData = new byte[(int) payloadLength];
            System.arraycopy(bytes, headerOffset, payloadData, 0, (int) payloadLength);
        } else {
            payloadData = new byte[0];
        }
    }

    @Override
    public void writeFrameMaskHeader(ObjectOutput out) throws IOException {
        var maskByte = isMasked ? (byte) 0x80 : (byte) 0x00;
        var secondByte = (byte) (maskByte | (byte) payloadLength);
        out.writeByte(secondByte);
    }

    @Override
    public int getTotalFrameLength() {
        return 2 + (isMasked ? 4 : 0) + (int) payloadLength;
    }

    @Override
    public void writePayloadLength(ObjectOutput out) {
        // note: control frames do not have a separate payload length field usually, it's in the mask byte
    }

    @Override
    public void writePayload(ObjectOutput out) throws IOException {
        if (payloadLength > 0 && payloadData != null) {
            out.write(payloadData);
        }
    }
}
