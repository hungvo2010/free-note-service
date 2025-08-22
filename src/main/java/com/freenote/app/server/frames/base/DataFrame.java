package com.freenote.app.server.frames.base;

import com.freenote.app.server.util.FrameUtil;

import java.io.IOException;
import java.io.ObjectOutput;
import java.util.Arrays;

import static com.freenote.app.server.util.FrameUtil.getFramePayloadLengthSupplier;
import static com.freenote.app.server.util.FrameUtil.getMaskingKeyStartSupplier;

public class DataFrame extends WebSocketFrame {

    private static final int DEFAULT_MASKING_KEY_LENGTH = 4;
    private static final int MAX_PAYLOAD_LENGTH_2_BYTES = 65536;
    private static final int MAX_PAYLOAD_LENGTH_7_BITS = 126;

    public DataFrame() {
        super(FrameTypeWithBehavior.TEXT.getOpcode());
    }

    public DataFrame(short opCode, byte[] bytes) {
        super(opCode, bytes);
    }

    public DataFrame(short opCode, byte[] bytes, boolean isMasked, byte[] maskingKey) {
        super(opCode, bytes);
        this.isMasked = isMasked;
        this.maskingKey = maskingKey;
    }

    public DataFrame(byte[] payload) {
        super(payload);
    }

    @Override
    protected void parsePayloadLength(byte[] bytes) {
        isMasked = ((bytes[1] & 0x80) >> 7) == 1;
        payloadLength = FrameUtil.parsePayloadLength(bytes);
    }

    @Override
    protected void parsePayload(byte[] bytes) {
        var maskingKeyStart = getMaskingKeyStartSupplier().applyAsInt(bytes[1]);
        payloadData = Arrays.copyOfRange(bytes, maskingKeyStart + maskingKey.length, bytes.length);
    }

    @Override
    public void writeFrameMaskHeader(ObjectOutput out) throws IOException {
        var maskByte = isMasked ? (byte) 0x80 : (byte) 0x00;
        var secondByte = (byte) (maskByte | (byte) (getFramePayloadLengthSupplier().applyAsLong(payloadLength)));
        out.writeByte(secondByte);
    }

    @Override
    public void writePayloadLength(ObjectOutput out) throws IOException {
        if (payloadLength < MAX_PAYLOAD_LENGTH_7_BITS) {
            return;
        }
        if (payloadLength < MAX_PAYLOAD_LENGTH_2_BYTES) {
            out.writeShort((short) payloadLength);
            return;
        }
        out.writeLong(payloadLength);
    }

    @Override
    public void parseMaskingKey(byte[] bytes) {
        var maskingKeyStart = getMaskingKeyStartSupplier().applyAsInt(bytes[1]);
        maskingKey = isMasked ? Arrays.copyOfRange(bytes, maskingKeyStart, maskingKeyStart + DEFAULT_MASKING_KEY_LENGTH) : new byte[0]; // Masking key is present if masked is true
    }

    @Override
    public void writePayload(ObjectOutput out) throws IOException {
        out.write(isMasked ? FrameUtil.maskPayload(payloadData, maskingKey) : payloadData);
    }
}
